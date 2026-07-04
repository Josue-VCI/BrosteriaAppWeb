-- Ejecutar en el SQL Editor de Supabase antes de desplegar.
-- El script es transaccional e idempotente: puede repetirse si una ejecucion previa fallo.
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
WHERE payment_status IS NULL
   OR payment_status NOT IN ('PENDIENTE', 'PAGADO');

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

CREATE INDEX IF NOT EXISTS idx_pedidos_paid_at
    ON pedidos (paid_at DESC)
    WHERE payment_status = 'PAGADO';

COMMIT;

SELECT
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'pedidos' AND column_name = 'request_id')
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'pedidos' AND column_name = 'payment_status')
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'pedidos' AND column_name = 'paid_at')
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'insumos' AND column_name = 'last_alerted_at')
    AS migration_ok;
