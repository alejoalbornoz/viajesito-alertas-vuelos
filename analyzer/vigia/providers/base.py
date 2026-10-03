from __future__ import annotations

from typing import Protocol

from ..models import FlightQuote, Route


class ProviderError(Exception):
    """Falla al consultar un proveedor (red, cuota, respuesta inválida...)."""


class PriceProvider(Protocol):
    name: str

    def fetch_lowest(self, route: Route) -> FlightQuote | None:
        """Devuelve la tarifa más baja disponible, o None si no hay resultados."""
        ...
