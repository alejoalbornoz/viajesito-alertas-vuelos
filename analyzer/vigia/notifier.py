"""Envío de ofertas detectadas: al backend Java (que decide y manda a Telegram)
o simplemente a consola."""

from __future__ import annotations

import logging
import time
from typing import Protocol

import requests

from .analysis import Evaluation
from .config import Settings
from .models import FlightQuote, Route

log = logging.getLogger(__name__)


class NotificationError(Exception):
    pass


class Notifier(Protocol):
    def notify(self, route: Route, quote: FlightQuote, price_history_id: int | None, evaluation: Evaluation) -> str:
        """Devuelve el estado final de la alerta (SENT, DRY_RUN, SUPPRESSED, ...)."""
        ...


def _round(value: float | None) -> float | None:
    return round(value, 2) if value is not None else None


def build_alert_payload(route: Route, quote: FlightQuote, price_history_id: int | None, ev: Evaluation) -> dict:
    s = ev.stats
    return {
        "routeId": route.id,
        "priceHistoryId": price_history_id,
        "price": _round(quote.price),
        "currency": quote.currency,
        "airline": quote.airline,
        "stops": quote.stops,
        "alertType": ev.alert_type,
        "reasons": ev.reasons,
        "averagePrice": _round(s.mean),
        "medianPrice": _round(s.median),
        "minPrice": _round(s.min),
        "savingsPct": _round(ev.savings_pct),
        "zscore": _round(ev.zscore),
        "sampleSize": s.sample_size,
        "trend": s.trend,
        "departureAirport": quote.departure_airport,
        "arrivalAirport": quote.arrival_airport,
        "googlePriceLevel": quote.google_price_level,
        "googleTypicalLow": _round(quote.google_typical_low),
        "googleTypicalHigh": _round(quote.google_typical_high),
    }


class BackendNotifier:
    """POST /api/alerts al backend Spring Boot. Reintenta solo errores de red/5xx."""

    def __init__(self, base_url: str, api_key: str = "", timeout: float = 15, retries: int = 3,
                 backoff_seconds: float = 5, session: requests.Session | None = None):
        self._url = f"{base_url}/api/alerts"
        self._headers = {"X-Api-Key": api_key} if api_key else {}
        self._timeout = timeout
        self._retries = retries
        self._backoff = backoff_seconds
        self._session = session or requests.Session()

    def notify(self, route: Route, quote: FlightQuote, price_history_id: int | None, evaluation: Evaluation) -> str:
        payload = build_alert_payload(route, quote, price_history_id, evaluation)
        last_problem = ""
        for attempt in range(1, self._retries + 1):
            try:
                resp = self._session.post(self._url, json=payload, headers=self._headers, timeout=self._timeout)
            except (requests.ConnectionError, requests.Timeout) as exc:
                last_problem = type(exc).__name__
            else:
                if resp.status_code < 300:
                    body = resp.json()
                    status = body.get("status", "UNKNOWN")
                    level = logging.WARNING if status == "FAILED" else logging.INFO
                    log.log(level, "Backend respondió %s para %s%s", status, route.label,
                            f" ({body['detail']})" if body.get("detail") else "")
                    return status
                if resp.status_code < 500:
                    raise NotificationError(f"Backend rechazó la alerta ({resp.status_code}): {resp.text[:300]}")
                last_problem = f"HTTP {resp.status_code}"

            if attempt < self._retries:
                wait = self._backoff * 2 ** (attempt - 1)
                log.warning("Backend no disponible (%s), reintento %d/%d en %.0fs",
                            last_problem, attempt, self._retries - 1, wait)
                time.sleep(wait)

        raise NotificationError(f"No se pudo contactar al backend tras {self._retries} intentos ({last_problem})")


class ConsoleNotifier:
    def notify(self, route: Route, quote: FlightQuote, price_history_id: int | None, evaluation: Evaluation) -> str:
        savings = f"{evaluation.savings_pct:+.1f}% vs promedio" if evaluation.savings_pct is not None else "sin histórico"
        log.info("🔔 OFERTA %s: %s %.2f (%s) · %s · señales: %s",
                 route.label, quote.currency, quote.price, quote.airline or "?", savings,
                 ", ".join(evaluation.reasons))
        return "CONSOLE"


def get_notifier(settings: Settings, force_console: bool = False) -> Notifier:
    if force_console or settings.notifier == "console":
        return ConsoleNotifier()
    if settings.notifier == "backend":
        return BackendNotifier(settings.backend_url, settings.api_key, timeout=settings.request_timeout)
    raise ValueError(f"NOTIFIER desconocido: {settings.notifier!r} (opciones: backend, console)")
