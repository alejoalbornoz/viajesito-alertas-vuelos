from datetime import datetime, timedelta, timezone

import pandas as pd
import pytest

from vigia.analysis import (
    ANOMALY,
    BELOW_AVERAGE,
    GOOGLE_LOW,
    HISTORICAL_MIN,
    TARGET_PRICE,
    TREND_DOWN,
    TREND_STABLE,
    TREND_UP,
    compute_stats,
    daily_prices,
    evaluate,
)
from vigia.config import AnalysisConfig

START = datetime(2026, 9, 1, 12, tzinfo=timezone.utc)


def history(prices, start=START):
    return pd.DataFrame({
        "checked_at": [start + timedelta(days=i) for i in range(len(prices))],
        "price": prices,
    })


def test_daily_prices_keeps_cheapest_sample_per_day():
    df = pd.DataFrame({
        "checked_at": [START, START + timedelta(hours=3), START + timedelta(days=1)],
        "price": [500.0, 450.0, 480.0],
    })
    assert daily_prices(df).tolist() == [450.0, 480.0]


def test_stats_on_empty_history():
    stats = compute_stats(history([]))
    assert stats.sample_size == 0
    assert stats.mean is None


def test_basic_stats():
    stats = compute_stats(history([100, 200, 300, 400]))
    assert stats.sample_size == 4
    assert stats.mean == 250
    assert stats.median == 250
    assert stats.min == 100
    assert stats.max == 400
    assert stats.last == 400
    assert stats.day_change_pct == pytest.approx(33.33, abs=0.01)


@pytest.mark.parametrize("prices, expected", [
    ([500, 480, 460, 440, 420], TREND_DOWN),
    ([400, 420, 440, 460, 480], TREND_UP),
    ([500, 502, 499, 501, 500], TREND_STABLE),
])
def test_trend(prices, expected):
    assert compute_stats(history(prices)).trend == expected


def test_no_statistical_signals_without_enough_samples():
    ev = evaluate(100, history([500, 500, 500]), config=AnalysisConfig(min_samples=5))
    assert not ev.is_deal
    assert ev.savings_pct == 80.0  # el ahorro se informa igual


def test_below_average_signal():
    ev = evaluate(800, history([1000, 1010, 990, 1005, 995, 1000]), config=AnalysisConfig(deal_threshold_pct=15))
    assert BELOW_AVERAGE in ev.reasons
    assert ev.savings_pct == pytest.approx(20.0, abs=0.1)


def test_historical_min_and_anomaly_have_priority():
    ev = evaluate(700, history([1000, 1020, 980, 1010, 990, 1000]), target_price_max=750)
    assert ev.reasons == [HISTORICAL_MIN, ANOMALY, BELOW_AVERAGE, TARGET_PRICE]
    assert ev.alert_type == HISTORICAL_MIN
    assert ev.zscore < -2


def test_tiny_new_low_on_stable_route_is_not_a_deal():
    # 960 es mínimo y z=-2.8, pero solo ahorra 4% (< MIN_SAVINGS_PCT).
    ev = evaluate(960, history([1000, 1020, 980, 1010, 990, 1000]))
    assert not ev.is_deal
    assert ev.zscore < -2


def test_material_new_low_without_reaching_average_threshold():
    ev = evaluate(940, history([1000, 1020, 980, 1010, 990, 1000]))
    assert ev.reasons == [HISTORICAL_MIN, ANOMALY]


def test_flat_prices_do_not_trigger_historical_min():
    ev = evaluate(500, history([500] * 10))
    assert HISTORICAL_MIN not in ev.reasons
    assert ev.zscore is None  # desvío 0: no hay z-score


def test_same_day_samples_are_excluded_from_baseline():
    df = history([1000, 1020, 980, 1010, 990, 1000, 700])  # el último es "hoy"
    today = START + timedelta(days=6, hours=5)
    ev = evaluate(700, df, as_of=today)
    assert ev.stats.sample_size == 6
    assert HISTORICAL_MIN in ev.reasons


def test_google_low_works_without_history():
    ev = evaluate(300, history([]), google_price_level="low")
    assert ev.reasons == [GOOGLE_LOW]


def test_google_typical_or_high_is_not_a_signal():
    assert not evaluate(300, history([]), google_price_level="typical").is_deal
    assert not evaluate(300, history([]), google_price_level="high").is_deal


def test_google_low_goes_after_statistical_signals():
    ev = evaluate(700, history([1000, 1020, 980, 1010, 990, 1000]), target_price_max=750,
                  google_price_level="low")
    assert ev.reasons == [HISTORICAL_MIN, ANOMALY, BELOW_AVERAGE, GOOGLE_LOW, TARGET_PRICE]


def test_target_price_works_without_history():
    ev = evaluate(650, history([]), target_price_max=700)
    assert ev.reasons == [TARGET_PRICE]
    assert ev.savings_pct is None
