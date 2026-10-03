-- =====================================================================
-- Viajesito · Esquema base
-- Se ejecuta automáticamente la primera vez que se crea el volumen de
-- PostgreSQL (docker-entrypoint-initdb.d). Para re-aplicarlo:
--   docker compose down -v && docker compose up -d
-- =====================================================================

-- Rutas a monitorear ---------------------------------------------------
CREATE TABLE routes (
    id                BIGSERIAL     PRIMARY KEY,
    origin            VARCHAR(3)    NOT NULL,              -- IATA (EZE)
    destination       VARCHAR(3)    NOT NULL,              -- IATA (MAD)
    departure_date    DATE          NOT NULL,
    return_date       DATE,                                -- NULL = solo ida
    adults            INTEGER       NOT NULL DEFAULT 1,
    max_stops         INTEGER,                             -- NULL = cualquiera, 0 = solo directos
    currency          VARCHAR(3)    NOT NULL DEFAULT 'USD',
    target_price_min  NUMERIC(10,2),                       -- "precio soñado"
    target_price_max  NUMERIC(10,2),                       -- umbral aceptable: alerta si precio <= esto
    active            BOOLEAN       NOT NULL DEFAULT TRUE,
    notes             VARCHAR(255),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT routes_origin_iata      CHECK (origin ~ '^[A-Z]{3}$'),
    CONSTRAINT routes_destination_iata CHECK (destination ~ '^[A-Z]{3}$'),
    CONSTRAINT routes_distinct_airports CHECK (origin <> destination),
    CONSTRAINT routes_return_after_departure CHECK (return_date IS NULL OR return_date >= departure_date),
    CONSTRAINT routes_adults_positive  CHECK (adults BETWEEN 1 AND 9),
    CONSTRAINT routes_max_stops        CHECK (max_stops IS NULL OR max_stops BETWEEN 0 AND 2),
    CONSTRAINT routes_target_range     CHECK (target_price_min IS NULL OR target_price_max IS NULL
                                              OR target_price_min <= target_price_max)
);

CREATE INDEX idx_routes_active ON routes (active, departure_date);

-- Histórico de sondeos de precio ---------------------------------------
CREATE TABLE price_history (
    id                BIGSERIAL     PRIMARY KEY,
    route_id          BIGINT        NOT NULL REFERENCES routes (id) ON DELETE CASCADE,
    price             NUMERIC(10,2) NOT NULL CHECK (price > 0),
    currency          VARCHAR(3)    NOT NULL,
    airline           VARCHAR(100),
    stops             INTEGER,
    duration_minutes  INTEGER,
    source            VARCHAR(50)   NOT NULL,              -- proveedor: mock, serpapi, ...
    checked_at        TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_price_history_route_time ON price_history (route_id, checked_at DESC);

-- Historial de alertas enviadas (anti-spam) ----------------------------
CREATE TABLE alerts_log (
    id                BIGSERIAL     PRIMARY KEY,
    route_id          BIGINT        NOT NULL REFERENCES routes (id) ON DELETE CASCADE,
    price_history_id  BIGINT        REFERENCES price_history (id) ON DELETE SET NULL,
    alert_type        VARCHAR(30)   NOT NULL,              -- HISTORICAL_MIN, ANOMALY, BELOW_AVERAGE, TARGET_PRICE
    reasons           VARCHAR(200),                        -- todas las señales, separadas por coma
    price             NUMERIC(10,2) NOT NULL,
    currency          VARCHAR(3)    NOT NULL,
    reference_price   NUMERIC(10,2),                       -- promedio histórico usado como referencia
    savings_pct       NUMERIC(6,2),
    status            VARCHAR(20)   NOT NULL,              -- SENT, DRY_RUN, FAILED
    message           TEXT,
    error             VARCHAR(500),
    sent_at           TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_alerts_log_route_time ON alerts_log (route_id, sent_at DESC);
