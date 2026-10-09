from dataclasses import replace
from datetime import date, datetime, timezone

import pytest

from vigia.models import Route
from vigia.providers import ProviderError, SerpApiProvider
from vigia.providers.mock import simulate_quote
from vigia.providers.serpapi import parse_response

ROUTE = Route(id=1, origin="EZE", destination="MAD", departure_date=date(2027, 1, 15),
              return_date=date(2027, 1, 30), adults=1, currency="USD",
              target_price_min=900, target_price_max=1100)


def test_mock_is_deterministic_per_day():
    morning = datetime(2026, 10, 2, 8, tzinfo=timezone.utc)
    night = datetime(2026, 10, 2, 22, tzinfo=timezone.utc)
    next_day = datetime(2026, 10, 3, 8, tzinfo=timezone.utc)
    assert simulate_quote(ROUTE, morning) == simulate_quote(ROUTE, night)
    assert simulate_quote(ROUTE, morning).price != simulate_quote(ROUTE, next_day).price


def test_mock_prices_are_plausible():
    quote = simulate_quote(ROUTE, datetime(2026, 10, 2, tzinfo=timezone.utc))
    assert 500 < quote.price < 2500
    assert quote.currency == "USD"
    assert quote.source == "mock"


def seg(airline, dep, arr):
    return {"airline": airline, "departure_airport": {"id": dep}, "arrival_airport": {"id": arr}}


SERPAPI_SAMPLE = {
    "best_flights": [
        {"price": 1250, "total_duration": 780,
         "flights": [seg("Iberia", "EZE", "MAD")]},
    ],
    "other_flights": [
        {"price": 1032, "total_duration": 1105,
         "flights": [seg("LATAM", "AEP", "GRU"), seg("LATAM", "GRU", "LIS"), seg("Iberia", "LIS", "MAD")]},
        {"total_duration": 900, "flights": [seg("Sin precio", "EZE", "MAD")]},
    ],
    "price_insights": {"lowest_price": 1032, "price_level": "typical", "typical_price_range": [950, 1300]},
}


def test_parse_serpapi_picks_cheapest_option():
    quote = parse_response(SERPAPI_SAMPLE, "USD")
    assert quote.price == 1032
    assert quote.airline == "LATAM / Iberia"
    assert quote.stops == 2
    assert quote.duration_minutes == 1105
    assert quote.source == "serpapi"
    assert (quote.departure_airport, quote.arrival_airport) == ("AEP", "MAD")


def test_parse_serpapi_google_insights():
    quote = parse_response(SERPAPI_SAMPLE, "USD")
    assert quote.google_price_level == "typical"
    assert (quote.google_typical_low, quote.google_typical_high) == (950, 1300)

    bare = parse_response({"best_flights": SERPAPI_SAMPLE["best_flights"]}, "USD")
    assert bare.google_price_level is None
    assert bare.google_typical_low is None


def test_serpapi_multi_airport_origin_is_sent_as_is():
    session = FakeSession(FakeResponse(200, SERPAPI_SAMPLE))
    SerpApiProvider("secret", session=session).fetch_lowest(replace(ROUTE, origin="EZE,AEP"))
    assert session.calls[0]["departure_id"] == "EZE,AEP"


def test_mock_picks_one_of_the_route_airports():
    quote = simulate_quote(replace(ROUTE, origin="EZE,AEP"), datetime(2026, 10, 2, tzinfo=timezone.utc))
    assert quote.departure_airport in {"EZE", "AEP"}
    assert quote.arrival_airport == "MAD"


def test_parse_serpapi_respects_max_stops():
    quote = parse_response(SERPAPI_SAMPLE, "USD", max_stops=0)
    assert quote.price == 1250  # la de 1032 tiene 2 escalas
    assert quote.stops == 0
    assert parse_response(SERPAPI_SAMPLE, "USD", max_stops=2).price == 1032


def test_serpapi_sends_stops_filter_for_direct_only():
    session = FakeSession(FakeResponse(200, SERPAPI_SAMPLE))
    SerpApiProvider("secret", session=session).fetch_lowest(replace(ROUTE, max_stops=0))
    assert session.calls[0]["stops"] == 1


def test_parse_serpapi_without_prices():
    assert parse_response({"best_flights": [], "other_flights": []}, "USD") is None


class FakeResponse:
    def __init__(self, status_code, payload):
        self.status_code = status_code
        self._payload = payload

    def json(self):
        return self._payload


class FakeSession:
    def __init__(self, response):
        self.response = response
        self.calls = []

    def get(self, url, params, timeout):
        self.calls.append(params)
        return self.response


def test_serpapi_request_params_for_round_trip():
    session = FakeSession(FakeResponse(200, SERPAPI_SAMPLE))
    SerpApiProvider("secret", session=session).fetch_lowest(ROUTE)
    params = session.calls[0]
    assert params["engine"] == "google_flights"
    assert params["departure_id"] == "EZE"
    assert params["arrival_id"] == "MAD"
    assert params["type"] == "1"
    assert params["return_date"] == "2027-01-30"


def test_serpapi_no_results_returns_none():
    error = {"error": "Google Flights hasn't returned any results for this query."}
    provider = SerpApiProvider("secret", session=FakeSession(FakeResponse(200, error)))
    assert provider.fetch_lowest(ROUTE) is None


def test_serpapi_error_does_not_leak_key():
    provider = SerpApiProvider("secret", session=FakeSession(FakeResponse(401, {"error": "Invalid API key."})))
    with pytest.raises(ProviderError) as exc:
        provider.fetch_lowest(ROUTE)
    assert "secret" not in str(exc.value)
