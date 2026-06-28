-- Ejecutar en Supabase antes de desplegar esta version.
BEGIN;

ALTER TABLE pedidos
    ADD COLUMN IF NOT EXISTS request_id VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS uq_pedidos_request_id
    ON pedidos (request_id)
    WHERE request_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_pedidos_telefono_estado
    ON pedidos (customer_phone, status);

CREATE INDEX IF NOT EXISTS idx_pedidos_estado_fecha
    ON pedidos (status, order_date DESC);

COMMIT;
