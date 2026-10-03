"""Configuración leída de variables de entorno (y de un `.env` si existe)."""

from __future__ import annotations

import os
from dataclasses import dataclass, field

from dotenv import find_dotenv, load_dotenv

# Busca un .env desde el directorio actual hacia arriba (sirve tanto desde
# `analyzer/` como desde la raíz del proyecto). No pisa variables ya definidas.
load_dotenv(find_dotenv(usecwd=True))


def _env(name: str, default: str = "") -> str:
    return os.getenv(name, default).strip()


def _default_database_url() -> str:
    user = _env("POSTGRES_USER", "viajesito")
    password = _env("POSTGRES_PASSWORD", "viajesito")
    port = _env("POSTGRES_PORT", "5432")
    db = _env("POSTGRES_DB", "viajesito")
    return f"postgresql://{user}:{password}@localhost:{port}/{db}"


@dataclass(frozen=True)
class AnalysisConfig:
    deal_threshold_pct: float = 15.0
    min_savings_pct: float = 5.0
    min_samples: int = 5
    zscore_threshold: float = 2.0
    trend_window_days: int = 14


@dataclass(frozen=True)
class Settings:
    database_url: str
    price_provider: str
    serpapi_api_key: str
    notifier: str
    backend_url: str
    api_key: str
    run_at: str
    run_on_start: bool
    request_timeout: float
    analysis: AnalysisConfig = field(default_factory=AnalysisConfig)

    @classmethod
    def from_env(cls) -> Settings:
        return cls(
            database_url=_env("DATABASE_URL") or _default_database_url(),
            price_provider=_env("PRICE_PROVIDER", "mock").lower(),
            serpapi_api_key=_env("SERPAPI_API_KEY"),
            notifier=_env("NOTIFIER", "backend").lower(),
            backend_url=_env("BACKEND_URL", "http://localhost:8080").rstrip("/"),
            api_key=_env("APP_API_KEY"),
            run_at=_env("RUN_AT", "08:00"),
            run_on_start=_env("RUN_ON_START", "true").lower() in {"1", "true", "yes", "si", "sí"},
            request_timeout=float(_env("REQUEST_TIMEOUT", "30")),
            analysis=AnalysisConfig(
                deal_threshold_pct=float(_env("DEAL_THRESHOLD_PCT", "15")),
                min_savings_pct=float(_env("MIN_SAVINGS_PCT", "5")),
                min_samples=int(_env("MIN_SAMPLES", "5")),
                zscore_threshold=float(_env("ZSCORE_THRESHOLD", "2.0")),
            ),
        )
