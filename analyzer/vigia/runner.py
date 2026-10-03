"""Orquestación de una corrida: recolectar → guardar → analizar → notificar."""

from __future__ import annotations

import logging
from dataclasses import dataclass, replace
from datetime import datetime, timedelta, timezone

import pandas as pd

from . import analysis, db
from .config import Settings
from .models import Route
from .notifier import NotificationError, Notifier, get_notifier
from .providers import PriceProvider, ProviderError, get_provider
from .providers.mock import simulate_quote

log = logging.getLogger(__name__)


@dataclass
class RunSummary:
    routes: int = 0
    skipped: int = 0
    saved: int = 0
    deals: int = 0
    notified: int = 0
    errors: int = 0

    def __str__(self) -> str:
        return (f"{self.routes} rutas ({self.skipped} ya consultadas hoy) · {self.saved} precios guardados · "
                f"{self.deals} ofertas · {self.notified} notificadas · {self.errors} errores")


def _start_of_today() -> datetime:
    return datetime.now().astimezone().replace(hour=0, minute=0, second=0, microsecond=0)


def _process_route(conn, route: Route, provider: PriceProvider, notifier: Notifier,
                   settings: Settings, dry_run: bool, summary: RunSummary) -> None:
    quote = provider.fetch_lowest(route)
    if quote is None:
        log.info("%s: sin resultados del proveedor", route.label)
        return
    if quote.currency != route.currency:
        log.warning("%s: el proveedor devolvió %s y la ruta usa %s; el precio no se comparará con el histórico",
                    route.label, quote.currency, route.currency)

    history = db.fetch_price_history(conn, route.id, route.currency,
                                     include_simulated=provider.name == "mock")
    price_id = None
    if not dry_run:
        price_id = db.insert_price(conn, route.id, quote)
        conn.commit()
        summary.saved += 1

    ev = analysis.evaluate(quote.price, history, route.target_price_max, settings.analysis,
                           as_of=datetime.now(timezone.utc))
    s = ev.stats
    log.info("%s: %s %.2f (%s) | n=%d prom=%s mín=%s tendencia=%s",
             route.label, quote.currency, quote.price, quote.airline or "?", s.sample_size,
             f"{s.mean:.2f}" if s.mean is not None else "-",
             f"{s.min:.2f}" if s.min is not None else "-", s.trend or "-")

    if ev.is_deal:
        summary.deals += 1
        status = notifier.notify(route, quote, price_id, ev)
        if status in {"SENT", "DRY_RUN", "CONSOLE"}:
            summary.notified += 1


def run_once(settings: Settings, route_id: int | None = None, dry_run: bool = False,
             force: bool = False) -> RunSummary:
    """Una pasada completa sobre las rutas activas. `dry_run` no escribe en la base
    y muestra las ofertas por consola en lugar de enviarlas.

    Salvo `force` o `dry_run`, cada ruta se consulta como máximo una vez por día con
    el mismo proveedor: reiniciar la PC varias veces no gasta búsquedas de más."""
    provider = get_provider(settings)
    notifier = get_notifier(settings, force_console=dry_run)
    summary = RunSummary()
    log.info("Inicio de corrida (proveedor=%s%s)", provider.name, ", dry-run" if dry_run else "")

    with db.connect(settings.database_url) as conn:
        routes = db.fetch_routes(conn, route_id)
        summary.routes = len(routes)
        if not routes:
            log.warning("No hay rutas activas con fecha futura para vigilar")
        already_checked = set() if (force or dry_run) else db.route_ids_checked_since(
            conn, provider.name, _start_of_today())
        for route in routes:
            if route.id in already_checked:
                summary.skipped += 1
                log.info("%s: ya se consultó hoy con %s, se omite (usá --force para repetir)",
                         route.label, provider.name)
                continue
            try:
                _process_route(conn, route, provider, notifier, settings, dry_run, summary)
            except (ProviderError, NotificationError) as exc:
                summary.errors += 1
                log.error("%s: %s", route.label, exc)
            except Exception:
                summary.errors += 1
                conn.rollback()
                log.exception("%s: error inesperado", route.label)

    log.info("Fin de corrida: %s", summary)
    return summary


def route_stats_table(settings: Settings, route_id: int | None = None) -> pd.DataFrame:
    rows = []
    with db.connect(settings.database_url) as conn:
        for route in db.fetch_routes(conn, route_id, only_active=False):
            history = db.fetch_price_history(conn, route.id, route.currency)
            s = analysis.compute_stats(history, settings.analysis.trend_window_days)
            rows.append({
                "ruta": route.label,
                "días": s.sample_size,
                "último": s.last,
                "Δ día %": s.day_change_pct,
                "promedio": s.mean,
                "mediana": s.median,
                "mínimo": s.min,
                "máximo": s.max,
                "CV %": s.cv_pct,
                "tendencia": s.trend,
                "Δ semana %": s.weekly_change_pct,
                "objetivo": route.target_price_max,
            })
    return pd.DataFrame(rows)


def backfill(settings: Settings, days: int, route_id: int | None = None) -> int:
    """Genera `days` días de histórico SIMULADO (source='mock-backfill') para
    poder probar el análisis sin esperar semanas de sondeos reales."""
    now = datetime.now(timezone.utc)
    total = 0
    with db.connect(settings.database_url) as conn:
        for route in db.fetch_routes(conn, route_id):
            points = []
            for d in range(days, 0, -1):
                at = now - timedelta(days=d)
                points.append((replace(simulate_quote(route, at), source="mock-backfill"), at))
            total += db.insert_prices(conn, route.id, points)
            log.info("%s: %d precios simulados", route.label, len(points))
        conn.commit()
    return total
