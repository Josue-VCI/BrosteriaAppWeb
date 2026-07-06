-- Ejecutar en Supabase antes de desplegar el backend con extra de chaufa.
BEGIN;

ALTER TABLE detalle_pedidos
    ADD COLUMN IF NOT EXISTS extra_chaufa BOOLEAN;

UPDATE detalle_pedidos
SET extra_chaufa = FALSE
WHERE extra_chaufa IS NULL;

ALTER TABLE detalle_pedidos
    ALTER COLUMN extra_chaufa SET DEFAULT FALSE,
    ALTER COLUMN extra_chaufa SET NOT NULL;

COMMIT;

SELECT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_name = 'detalle_pedidos'
      AND column_name = 'extra_chaufa'
) AS migration_ok;
