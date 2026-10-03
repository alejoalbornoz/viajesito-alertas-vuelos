"""CLI del Vigía.

    python -m vigia run [--route ID] [--dry-run] [--force]   una corrida ahora
    python -m vigia schedule                       corre todos los días a RUN_AT
    python -m vigia stats [--route ID]             estadísticas por ruta
    python -m vigia backfill --days 60             histórico simulado para pruebas
"""

from __future__ import annotations

import argparse
import logging
import sys

import pandas as pd

from .config import Settings
from .runner import backfill, route_stats_table, run_once
from .scheduler import run_forever


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(prog="vigia", description="Vigía y analista de precios de vuelos")
    sub = parser.add_subparsers(dest="command", required=True)

    run = sub.add_parser("run", help="Recolecta, analiza y notifica una vez")
    run.add_argument("--route", type=int, help="Solo esta ruta (id)")
    run.add_argument("--dry-run", action="store_true", help="No guarda en la base; ofertas solo por consola")
    run.add_argument("--force", action="store_true", help="Consulta aunque la ruta ya se haya consultado hoy")

    sub.add_parser("schedule", help="Ejecuta todos los días a la hora RUN_AT")

    stats = sub.add_parser("stats", help="Muestra estadísticas del histórico")
    stats.add_argument("--route", type=int, help="Solo esta ruta (id)")

    fill = sub.add_parser("backfill", help="Genera histórico simulado (solo para pruebas)")
    fill.add_argument("--days", type=int, default=60)
    fill.add_argument("--route", type=int, help="Solo esta ruta (id)")
    return parser


def main(argv: list[str] | None = None) -> int:
    # En Windows la salida redirigida usa cp1252 y falla con caracteres como "→".
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(encoding="utf-8", errors="replace")
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)-7s %(name)s: %(message)s",
                        stream=sys.stdout)
    args = _parser().parse_args(argv)
    settings = Settings.from_env()

    if args.command == "run":
        summary = run_once(settings, route_id=args.route, dry_run=args.dry_run, force=args.force)
        return 1 if summary.errors else 0
    if args.command == "schedule":
        run_forever(settings)
        return 0
    if args.command == "stats":
        table = route_stats_table(settings, args.route)
        if table.empty:
            print("No hay rutas cargadas.")
        else:
            with pd.option_context("display.width", 200, "display.max_columns", None, "display.precision", 2):
                print(table.to_string(index=False))
        return 0
    if args.command == "backfill":
        total = backfill(settings, args.days, args.route)
        print(f"{total} precios simulados insertados (source='mock-backfill').")
        return 0
    return 2


if __name__ == "__main__":
    sys.exit(main())
