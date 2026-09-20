-- Aplicar manualmente solo si las tablas del modelo anterior ya existen.
-- No contiene credenciales ni elimina movimientos. No se ejecuta al iniciar.
BEGIN;
ALTER TABLE movimientos_inventario ALTER COLUMN usuario_id DROP NOT NULL;
CREATE INDEX IF NOT EXISTS idx_movimientos_producto_fecha
    ON movimientos_inventario (producto_id, fecha, id);
COMMIT;
