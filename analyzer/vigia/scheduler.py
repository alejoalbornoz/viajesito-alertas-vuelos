"""Ejecución diaria sin dependencias externas (pensado para el contenedor)."""

from __future__ import annotations

import logging
import time
from datetime import datetime, timedelta

from .config import Settings
from .runner import run_once

log = logging.getLogger(__name__)


def next_run(run_at: str, now: datetime) -> datetime:
    hour, minute = (int(part) for part in run_at.split(":"))
    target = now.replace(hour=hour, minute=minute, second=0, microsecond=0)
    return target if target > now else target + timedelta(days=1)


def _sleep_until(target: datetime) -> None:
    """Duerme en tramos cortos comparando contra el reloj real: si la PC se suspende,
    `time.sleep` no avanza durante la suspensión y un único sleep largo se atrasaría."""
    while (remaining := (target - datetime.now()).total_seconds()) > 0:
        time.sleep(min(remaining, 60))


def _safe_run(settings: Settings) -> None:
    try:
        run_once(settings)
    except Exception:
        # Un fallo (p. ej. la base caída) no debe matar el proceso programado.
        log.exception("La corrida falló; se reintentará en el próximo horario")


def run_forever(settings: Settings) -> None:
    log.info("Modo programado: todos los días a las %s", settings.run_at)
    if settings.run_on_start:
        _safe_run(settings)
    while True:
        target = next_run(settings.run_at, datetime.now())
        log.info("Próxima ejecución: %s", target.strftime("%Y-%m-%d %H:%M"))
        _sleep_until(target)
        _safe_run(settings)
