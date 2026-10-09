-- =====================================================================
-- Migración 2026-10-09: rutas con varios aeropuertos + contexto de Google
--
-- Solo hace falta en bases creadas ANTES de esta fecha (las nuevas ya
-- nacen con este esquema desde db/init/01_schema.sql). Aplicar con:
--   docker compose exec -T db psql -U viajesito -d viajesito < db/migrations/2026-10-09_multi_aeropuerto_google.sql
-- =====================================================================
BEGIN;

-- Rutas: origin/destination admiten hasta 4 aeropuertos ("EZE,AEP").
ALTER TABLE routes
    ALTER COLUMN origin TYPE VARCHAR(15),
    ALTER COLUMN destination TYPE VARCHAR(15),
    DROP CONSTRAINT routes_origin_iata,
    DROP CONSTRAINT routes_destination_iata,
    ADD CONSTRAINT routes_origin_iata CHECK (origin ~ '^[A-Z]{3}(,[A-Z]{3}){0,3}$'),
    ADD CONSTRAINT routes_destination_iata CHECK (destination ~ '^[A-Z]{3}(,[A-Z]{3}){0,3}$');

-- Sondeos: aeropuerto concreto + opinión de Google Flights.
ALTER TABLE price_history
    ADD COLUMN departure_airport VARCHAR(3),
    ADD COLUMN arrival_airport VARCHAR(3),
    ADD COLUMN google_price_level VARCHAR(10),
    ADD COLUMN google_typical_low NUMERIC(10,2),
    ADD COLUMN google_typical_high NUMERIC(10,2);

-- En el histórico existente las rutas eran de un solo aeropuerto: ya se sabe cuál fue.
UPDATE price_history p
SET departure_airport = r.origin, arrival_airport = r.destination
FROM routes r
WHERE p.route_id = r.id AND length(r.origin) = 3 AND length(r.destination) = 3;

COMMIT;
