-- Ejecutar una sola vez en el SQL Editor de Supabase antes de desplegar.
BEGIN;

ALTER TABLE pedidos
    ADD COLUMN IF NOT EXISTS request_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS payment_status VARCHAR(20),
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP;

ALTER TABLE insumos
    ADD COLUMN IF NOT EXISTS last_alerted_at TIMESTAMP;

UPDATE pedidos
SET payment_status = CASE
    WHEN status = 'ENTREGADO' THEN 'PAGADO'
    ELSE 'PENDIENTE'
END
WHERE payment_status IS NULL OR payment_status = '';

UPDATE pedidos
SET paid_at = order_date
WHERE payment_status = 'PAGADO' AND paid_at IS NULL;

ALTER TABLE pedidos
    ALTER COLUMN payment_status SET DEFAULT 'PENDIENTE',
    ALTER COLUMN payment_status SET NOT NULL;

ALTER TABLE pedidos
    DROP CONSTRAINT IF EXISTS chk_pedidos_payment_status;

ALTER TABLE pedidos
    ADD CONSTRAINT chk_pedidos_payment_status
    CHECK (payment_status IN ('PENDIENTE', 'PAGADO'));

CREATE UNIQUE INDEX IF NOT EXISTS uq_pedidos_request_id
    ON pedidos (request_id)
    WHERE request_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_pedidos_estado_fecha
    ON pedidos (status, order_date DESC);

CREATE INDEX IF NOT EXISTS idx_pedidos_pago_fecha
    ON pedidos (payment_status, order_date DESC);

COMMIT;
