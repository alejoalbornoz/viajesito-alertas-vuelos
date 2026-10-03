"""Acceso a PostgreSQL: lectura de rutas y lectura/escritura del histórico."""

from __future__ import annotations

from collections.abc import Iterable
from datetime import datetime

import pandas as pd
import psycopg
from psycopg.rows import dict_row

from .models import FlightQuote, Route

SIMULATED_SOURCE_PREFIX = "mock"


def connect(database_url: str) -> psycopg.Connection:
    return psycopg.connect(database_url, row_factory=dict_row)


def _to_float(value) -> float | None:
    return float(value) if value is not None else None


def _row_to_route(row: dict) -> Route:
    return Route(
        id=row["id"],
        origin=row["origin"],
        destination=row["destination"],
        departure_date=row["departure_date"],
        return_date=row["return_date"],
        adults=row["adults"],
        currency=row["currency"],
        target_price_min=_to_float(row["target_price_min"]),
        target_price_max=_to_float(row["target_price_max"]),
        max_stops=row["max_stops"],
    )


def fetch_routes(conn: psycopg.Connection, route_id: int | None = None, only_active: bool = True) -> list[Route]:
    """Rutas a vigilar. Con `only_active` excluye pausadas y con fecha de salida pasada."""
    sql = "SELECT * FROM routes WHERE TRUE"
    params: list = []
    if only_active:
        sql += " AND active AND departure_date >= CURRENT_DATE"
    if route_id is not None:
        sql += " AND id = %s"
        params.append(route_id)
    sql += " ORDER BY id"
    with conn.cursor() as cur:
        cur.execute(sql, params)
        return [_row_to_route(row) for row in cur.fetchall()]


def fetch_price_history(
    conn: psycopg.Connection,
    route_id: int,
    currency: str,
    include_simulated: bool = True,
) -> pd.DataFrame:
    """Histórico de una ruta como DataFrame con columnas `checked_at` y `price`."""
    sql = "SELECT checked_at, price FROM price_history WHERE route_id = %s AND currency = %s"
    params: list = [route_id, currency]
    if not include_simulated:
        sql += " AND source NOT LIKE %s"
        params.append(f"{SIMULATED_SOURCE_PREFIX}%")
    sql += " ORDER BY checked_at"
    with conn.cursor() as cur:
        cur.execute(sql, params)
        rows = cur.fetchall()
    df = pd.DataFrame(rows, columns=["checked_at", "price"])
    df["price"] = df["price"].astype(float)
    return df


def route_ids_checked_since(conn: psycopg.Connection, source: str, since: datetime) -> set[int]:
    """Rutas que ya tienen un sondeo de este proveedor desde `since`."""
    with conn.cursor() as cur:
        cur.execute("SELECT DISTINCT route_id FROM price_history WHERE source = %s AND checked_at >= %s",
                    (source, since))
        return {row["route_id"] for row in cur.fetchall()}


_INSERT_PRICE = """
    INSERT INTO price_history (route_id, price, currency, airline, stops, duration_minutes, source, checked_at)
    VALUES (%s, %s, %s, %s, %s, %s, %s, COALESCE(%s, now()))
    RETURNING id
"""


def _price_params(route_id: int, quote: FlightQuote, checked_at: datetime | None) -> tuple:
    return (
        route_id,
        round(quote.price, 2),
        quote.currency,
        quote.airline,
        quote.stops,
        quote.duration_minutes,
        quote.source,
        checked_at,
    )


def insert_price(conn: psycopg.Connection, route_id: int, quote: FlightQuote, checked_at: datetime | None = None) -> int:
    with conn.cursor() as cur:
        cur.execute(_INSERT_PRICE, _price_params(route_id, quote, checked_at))
        return cur.fetchone()["id"]


def insert_prices(conn: psycopg.Connection, route_id: int, quotes: Iterable[tuple[FlightQuote, datetime]]) -> int:
    params = [_price_params(route_id, quote, at) for quote, at in quotes]
    with conn.cursor() as cur:
        cur.executemany(_INSERT_PRICE.replace("RETURNING id", ""), params)
    return len(params)
