"""Precios reales de Google Flights a través de SerpApi (https://serpapi.com).

Plan gratuito: 250 búsquedas/mes. Cada ruta consume 1 búsqueda por sondeo,
así que un sondeo diario alcanza para ~8 rutas.
"""

from __future__ import annotations

import requests

from ..models import FlightQuote, Route
from .base import ProviderError

SEARCH_URL = "https://serpapi.com/search.json"
_NO_RESULTS_HINT = "hasn't returned any results"


def _stops(option: dict) -> int:
    return max(len(option.get("flights") or []) - 1, 0)


def parse_response(data: dict, currency: str, max_stops: int | None = None) -> FlightQuote | None:
    """Extrae la opción más barata de una respuesta del engine `google_flights`."""
    options = [*data.get("best_flights", []), *data.get("other_flights", [])]
    priced = [o for o in options if isinstance(o.get("price"), (int, float))
              and (max_stops is None or _stops(o) <= max_stops)]
    if not priced:
        return None

    best = min(priced, key=lambda o: o["price"])
    segments = best.get("flights") or []
    airlines = list(dict.fromkeys(s["airline"] for s in segments if s.get("airline")))
    insights = data.get("price_insights") or {}
    level = insights.get("price_level")
    typical = insights.get("typical_price_range") or []
    has_range = len(typical) == 2 and all(isinstance(v, (int, float)) for v in typical)
    return FlightQuote(
        price=float(best["price"]),
        currency=currency,
        source="serpapi",
        airline=" / ".join(airlines) or None,
        stops=len(segments) - 1 if segments else None,
        duration_minutes=best.get("total_duration"),
        departure_airport=(segments[0].get("departure_airport") or {}).get("id") if segments else None,
        arrival_airport=(segments[-1].get("arrival_airport") or {}).get("id") if segments else None,
        google_price_level=level if level in {"low", "typical", "high"} else None,
        google_typical_low=float(typical[0]) if has_range else None,
        google_typical_high=float(typical[1]) if has_range else None,
    )


class SerpApiProvider:
    name = "serpapi"

    def __init__(self, api_key: str, timeout: float = 30, session: requests.Session | None = None):
        self._api_key = api_key
        self._timeout = timeout
        self._session = session or requests.Session()

    def _params(self, route: Route) -> dict:
        params = {
            "engine": "google_flights",
            "departure_id": route.origin,  # admite varios aeropuertos: "EZE,AEP"
            "arrival_id": route.destination,
            "outbound_date": route.departure_date.isoformat(),
            "type": "1" if route.return_date else "2",  # 1 = ida y vuelta, 2 = solo ida
            "currency": route.currency,
            "adults": route.adults,
            "hl": "es",
            "api_key": self._api_key,
        }
        if route.return_date:
            params["return_date"] = route.return_date.isoformat()
        if route.max_stops is not None:
            # SerpApi: 1 = solo directos, 2 = hasta 1 escala, 3 = hasta 2 escalas
            params["stops"] = route.max_stops + 1
        return params

    def fetch_lowest(self, route: Route) -> FlightQuote | None:
        try:
            resp = self._session.get(SEARCH_URL, params=self._params(route), timeout=self._timeout)
        except requests.RequestException as exc:
            # No incluimos la URL: lleva la API key en la query string.
            raise ProviderError(f"SerpApi no respondió: {type(exc).__name__}") from None

        try:
            data = resp.json()
        except ValueError:
            raise ProviderError(f"SerpApi HTTP {resp.status_code}: respuesta no es JSON") from None

        error = data.get("error")
        if error:
            if _NO_RESULTS_HINT in error:
                return None
            raise ProviderError(f"SerpApi HTTP {resp.status_code}: {error}")
        if resp.status_code != 200:
            raise ProviderError(f"SerpApi HTTP {resp.status_code}")

        return parse_response(data, route.currency, route.max_stops)
