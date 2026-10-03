"""Proveedores de precios. Para sumar uno nuevo, implementá `PriceProvider`
y registralo en `get_provider`."""

from __future__ import annotations

from ..config import Settings
from .base import PriceProvider, ProviderError
from .mock import MockProvider
from .serpapi import SerpApiProvider

__all__ = ["PriceProvider", "ProviderError", "MockProvider", "SerpApiProvider", "get_provider"]


def get_provider(settings: Settings) -> PriceProvider:
    name = settings.price_provider
    if name == "mock":
        return MockProvider()
    if name == "serpapi":
        if not settings.serpapi_api_key:
            raise ProviderError("PRICE_PROVIDER=serpapi requiere SERPAPI_API_KEY")
        return SerpApiProvider(settings.serpapi_api_key, timeout=settings.request_timeout)
    raise ProviderError(f"Proveedor desconocido: {name!r} (opciones: mock, serpapi)")
