-- Ejecutar en Supabase antes de desplegar este backend.
BEGIN;

ALTER TABLE productos
    ALTER COLUMN price TYPE NUMERIC(12,2) USING ROUND(price::numeric, 2);

ALTER TABLE pedidos
    ALTER COLUMN delivery_cost TYPE NUMERIC(12,2) USING ROUND(delivery_cost::numeric, 2),
    ALTER COLUMN total TYPE NUMERIC(12,2) USING ROUND(total::numeric, 2);

ALTER TABLE detalle_pedidos
    ALTER COLUMN subtotal TYPE NUMERIC(12,2) USING ROUND(subtotal::numeric, 2);

ALTER TABLE clientes
    ALTER COLUMN total_spent TYPE NUMERIC(14,2) USING ROUND(total_spent::numeric, 2),
    ALTER COLUMN total_spent SET DEFAULT 0;

ALTER TABLE insumos
    ALTER COLUMN quantity TYPE NUMERIC(14,3) USING ROUND(quantity::numeric, 3),
    ALTER COLUMN minimum_stock TYPE NUMERIC(14,3) USING ROUND(minimum_stock::numeric, 3),
    ADD COLUMN IF NOT EXISTS last_alerted_at TIMESTAMP NULL;

CREATE INDEX IF NOT EXISTS idx_pedidos_reportes
    ON pedidos (order_date, status, type);

CREATE INDEX IF NOT EXISTS idx_detalle_pedidos_pedido
    ON detalle_pedidos (pedido_id);

COMMIT;
