"""Tipos de dominio compartidos entre módulos."""

from __future__ import annotations

from dataclasses import dataclass
from datetime import date


@dataclass(frozen=True)
class Route:
    id: int
    origin: str
    destination: str
    departure_date: date
    return_date: date | None
    adults: int
    currency: str
    target_price_min: float | None
    target_price_max: float | None
    max_stops: int | None = None  # None = cualquiera, 0 = solo directos

    @property
    def label(self) -> str:
        trip = f"{self.departure_date:%d/%m/%Y}"
        if self.return_date:
            trip += f" - {self.return_date:%d/%m/%Y}"
        # origin/destination pueden ser varios aeropuertos: "EZE,AEP"
        return f"#{self.id} {self.origin.replace(',', '/')}→{self.destination.replace(',', '/')} ({trip})"


@dataclass(frozen=True)
class FlightQuote:
    """La tarifa más barata encontrada para una ruta en un sondeo."""

    price: float
    currency: str
    source: str
    airline: str | None = None
    stops: int | None = None
    duration_minutes: int | None = None
    departure_airport: str | None = None  # útil cuando la ruta tiene varios aeropuertos
    arrival_airport: str | None = None
    # Contexto que Google Flights da para la búsqueda (sin costo extra)
    google_price_level: str | None = None  # low | typical | high
    google_typical_low: float | None = None
    google_typical_high: float | None = None
