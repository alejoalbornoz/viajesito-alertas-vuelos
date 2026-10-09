-- =====================================================================
-- Migración 2026-10-03: máximo de escalas por ruta (0 = solo directos)
-- Solo para bases creadas antes de esa fecha. Aplicar con:
--   docker compose exec -T db psql -U viajesito -d viajesito < db/migrations/2026-10-03_solo_directos.sql
-- =====================================================================
ALTER TABLE routes
    ADD COLUMN max_stops INTEGER,
    ADD CONSTRAINT routes_max_stops CHECK (max_stops IS NULL OR max_stops BETWEEN 0 AND 2);
