"""Análisis estadístico del histórico y detección de ofertas.

Funciones puras (sin I/O) para poder testearlas con DataFrames armados a mano.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime

import numpy as np
import pandas as pd

from .config import AnalysisConfig

# Señales, en orden de prioridad (la primera presente define el tipo de alerta).
HISTORICAL_MIN = "HISTORICAL_MIN"
ANOMALY = "ANOMALY"
BELOW_AVERAGE = "BELOW_AVERAGE"
TARGET_PRICE = "TARGET_PRICE"

TREND_DOWN = "DOWN"
TREND_UP = "UP"
TREND_STABLE = "STABLE"
_TREND_WEEKLY_PCT = 2.0


@dataclass(frozen=True)
class RouteStats:
    sample_size: int
    mean: float | None = None
    median: float | None = None
    min: float | None = None
    max: float | None = None
    std: float | None = None
    cv_pct: float | None = None  # coeficiente de variación
    last: float | None = None
    day_change_pct: float | None = None  # último día vs. el anterior
    trend: str | None = None
    weekly_change_pct: float | None = None  # pendiente de la tendencia, en % semanal


@dataclass(frozen=True)
class Evaluation:
    price: float
    stats: RouteStats
    reasons: list[str] = field(default_factory=list)
    savings_pct: float | None = None  # ahorro vs. promedio histórico
    zscore: float | None = None

    @property
    def is_deal(self) -> bool:
        return bool(self.reasons)

    @property
    def alert_type(self) -> str | None:
        return self.reasons[0] if self.reasons else None


def daily_prices(history: pd.DataFrame) -> pd.Series:
    """Precio mínimo por día. Un valor por día evita que los días con varios
    sondeos pesen más en el promedio."""
    if history.empty:
        return pd.Series(dtype=float)
    series = pd.Series(
        history["price"].astype(float).to_numpy(),
        index=pd.to_datetime(history["checked_at"], utc=True),
    ).sort_index()
    return series.resample("D").min().dropna()


def _trend(daily: pd.Series) -> tuple[str | None, float | None]:
    if len(daily) < 3:
        return None, None
    x = (daily.index - daily.index[0]).days.to_numpy(dtype=float)
    y = daily.to_numpy(dtype=float)
    slope, _intercept = np.polyfit(x, y, 1)
    weekly_pct = float(slope * 7 / y.mean() * 100)
    if weekly_pct <= -_TREND_WEEKLY_PCT:
        trend = TREND_DOWN
    elif weekly_pct >= _TREND_WEEKLY_PCT:
        trend = TREND_UP
    else:
        trend = TREND_STABLE
    return trend, round(weekly_pct, 2)


def compute_stats(history: pd.DataFrame, trend_window_days: int = 14) -> RouteStats:
    daily = daily_prices(history)
    n = len(daily)
    if n == 0:
        return RouteStats(sample_size=0)

    mean = float(daily.mean())
    std = float(daily.std(ddof=1)) if n > 1 else 0.0
    day_change = None
    if n > 1:
        prev, last = daily.iloc[-2], daily.iloc[-1]
        day_change = round(float((last - prev) / prev * 100), 2)
    trend, weekly = _trend(daily.tail(trend_window_days))

    return RouteStats(
        sample_size=n,
        mean=mean,
        median=float(daily.median()),
        min=float(daily.min()),
        max=float(daily.max()),
        std=std,
        cv_pct=round(std / mean * 100, 2) if mean else None,
        last=float(daily.iloc[-1]),
        day_change_pct=day_change,
        trend=trend,
        weekly_change_pct=weekly,
    )


def prior_days(history: pd.DataFrame, as_of: datetime) -> pd.DataFrame:
    """Solo los sondeos de días (UTC) anteriores a `as_of`: si el script corre dos
    veces el mismo día, el segundo precio no se compara contra el primero."""
    if history.empty:
        return history
    day_start = pd.Timestamp(as_of).tz_convert("UTC").normalize()
    return history[pd.to_datetime(history["checked_at"], utc=True) < day_start]


def evaluate(
    price: float,
    history: pd.DataFrame,
    target_price_max: float | None = None,
    config: AnalysisConfig | None = None,
    as_of: datetime | None = None,
) -> Evaluation:
    """Compara un precio nuevo contra el histórico previo. Con `as_of`, ignora
    los sondeos del mismo día."""
    config = config or AnalysisConfig()
    if as_of is not None:
        history = prior_days(history, as_of)
    stats = compute_stats(history, config.trend_window_days)
    reasons: list[str] = []
    savings_pct = zscore = None

    if stats.sample_size > 0:
        savings_pct = round((stats.mean - price) / stats.mean * 100, 2)

    if stats.sample_size >= config.min_samples:
        if stats.std > 0:
            zscore = round((price - stats.mean) / stats.std, 2)
        # En rutas de precio muy estable, bajar unos pocos dólares ya es "mínimo"
        # y "anomalía"; exigir un ahorro mínimo evita alertas irrelevantes.
        material = savings_pct >= config.min_savings_pct
        if material and price <= stats.min:
            reasons.append(HISTORICAL_MIN)
        if material and zscore is not None and zscore <= -config.zscore_threshold:
            reasons.append(ANOMALY)
        if savings_pct >= config.deal_threshold_pct:
            reasons.append(BELOW_AVERAGE)

    if target_price_max is not None and price <= target_price_max:
        reasons.append(TARGET_PRICE)

    return Evaluation(price=price, stats=stats, reasons=reasons, savings_pct=savings_pct, zscore=zscore)
