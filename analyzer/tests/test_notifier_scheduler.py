from datetime import date, datetime

import pandas as pd
import pytest
import requests

from vigia.analysis import evaluate
from vigia.models import FlightQuote, Route
from vigia.notifier import BackendNotifier, NotificationError, build_alert_payload
from vigia.scheduler import next_run

ROUTE = Route(id=7, origin="EZE", destination="MIA", departure_date=date(2027, 1, 10), return_date=None,
              adults=1, currency="USD", target_price_min=None, target_price_max=700)
QUOTE = FlightQuote(price=650.0, currency="USD", source="mock", airline="LATAM", stops=1)
EVALUATION = evaluate(650.0, pd.DataFrame(columns=["checked_at", "price"]), target_price_max=700)


def test_payload_shape():
    payload = build_alert_payload(ROUTE, QUOTE, 99, EVALUATION)
    assert payload["routeId"] == 7
    assert payload["priceHistoryId"] == 99
    assert payload["alertType"] == "TARGET_PRICE"
    assert payload["reasons"] == ["TARGET_PRICE"]
    assert payload["averagePrice"] is None
    assert "googlePriceLevel" in payload and "departureAirport" in payload


class FakeResponse:
    def __init__(self, status_code, payload=None):
        self.status_code = status_code
        self._payload = payload or {}
        self.text = str(payload)

    def json(self):
        return self._payload


class FakeSession:
    def __init__(self, *outcomes):
        self.outcomes = list(outcomes)
        self.headers_seen = []

    def post(self, url, json, headers, timeout):
        self.headers_seen.append(headers)
        outcome = self.outcomes.pop(0)
        if isinstance(outcome, Exception):
            raise outcome
        return outcome


def notifier(session, **kwargs):
    return BackendNotifier("http://backend", api_key="k", backoff_seconds=0, session=session, **kwargs)


def test_backend_notifier_retries_network_errors():
    session = FakeSession(requests.ConnectionError(), FakeResponse(503), FakeResponse(200, {"status": "SENT"}))
    assert notifier(session).notify(ROUTE, QUOTE, 1, EVALUATION) == "SENT"
    assert session.headers_seen[0] == {"X-Api-Key": "k"}


def test_backend_notifier_does_not_retry_client_errors():
    session = FakeSession(FakeResponse(400, {"error": "bad"}), FakeResponse(200, {"status": "SENT"}))
    with pytest.raises(NotificationError):
        notifier(session).notify(ROUTE, QUOTE, 1, EVALUATION)
    assert len(session.outcomes) == 1


def test_backend_notifier_gives_up():
    session = FakeSession(*(requests.ConnectionError() for _ in range(3)))
    with pytest.raises(NotificationError):
        notifier(session, retries=3).notify(ROUTE, QUOTE, 1, EVALUATION)


@pytest.mark.parametrize("now, expected", [
    (datetime(2026, 10, 2, 7, 0), datetime(2026, 10, 2, 8, 0)),
    (datetime(2026, 10, 2, 8, 0), datetime(2026, 10, 3, 8, 0)),
    (datetime(2026, 10, 2, 23, 30), datetime(2026, 10, 3, 8, 0)),
])
def test_next_run(now, expected):
    assert next_run("08:00", now) == expected
