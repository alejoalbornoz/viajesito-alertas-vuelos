"""Proveedor simulado: genera precios verosímiles sin tocar ninguna API.

El precio de un día es determinístico (misma ruta + mismo día = mismo precio),
así que re-ejecutar el script no ensucia el histórico con ruido. Modela:
  - un precio base propio de cada ruta,
  - encarecimiento a medida que se acerca la fecha de salida,
  - una ondulación semanal y ruido aleatorio,
  - "gangas" ocasionales (~6% de los días) con 20-38% de descuento.
"""

from __future__ import annotations

import hashlib
import math
import random
from datetime import datetime, timezone

from ..models import FlightQuote, Route

_AIRLINES = ["Aerolíneas Argentinas", "LATAM", "Iberia", "American Airlines", "Air Europa", "Copa", "GOL"]


def _route_key(route: Route) -> str:
    return f"{route.origin}-{route.destination}-{route.departure_date}-{route.return_date}"


def base_price(route: Route) -> float:
    seed = int(hashlib.sha256(_route_key(route).encode()).hexdigest()[:12], 16)
    rng = random.Random(seed)
    if route.target_price_max:
        # Arranca algo por encima de lo que el usuario considera aceptable.
        return route.target_price_max * rng.uniform(1.10, 1.35)
    return rng.uniform(250, 1200) * (1.8 if route.return_date else 1.0)


def _advance_factor(days_left: int) -> float:
    if days_left < 21:
        return 1 + (21 - max(days_left, 0)) * 0.025  # hasta +52% en la última semana
    if days_left > 120:
        return 1 + min((days_left - 120) * 0.001, 0.08)  # leve sobreprecio si falta mucho
    return 1.0


def simulate_quote(route: Route, at: datetime) -> FlightQuote:
    day = at.date()
    rng = random.Random(f"{_route_key(route)}@{day.isoformat()}")
    days_left = (route.departure_date - day).days

    price = base_price(route)
    price *= _advance_factor(days_left)
    price *= 1 + 0.03 * math.sin(2 * math.pi * day.toordinal() / 7)
    price *= 1 + rng.gauss(0, 0.04)
    if rng.random() < 0.06:
        price *= rng.uniform(0.62, 0.80)

    stops = rng.choice([0, 1, 1, 2])
    if route.max_stops is not None:
        stops = min(stops, route.max_stops)
    return FlightQuote(
        price=round(price, 2),
        currency=route.currency,
        source="mock",
        airline=rng.choice(_AIRLINES),
        stops=stops,
        duration_minutes=int(rng.uniform(120, 900) + stops * 150),
        departure_airport=rng.choice(route.origin.split(",")),
        arrival_airport=rng.choice(route.destination.split(",")),
    )


class MockProvider:
    name = "mock"

    def fetch_lowest(self, route: Route) -> FlightQuote:
        return simulate_quote(route, datetime.now(timezone.utc))
