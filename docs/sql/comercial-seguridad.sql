-- Para tablas existentes del modelo previo. Revisar/aplicar manualmente.
-- Antes, aplicar inventario-ajustes.sql si aun no se aplico.
BEGIN;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS token_version bigint NOT NULL DEFAULT 0;
-- Falla si hay emails duplicados ignorando mayusculas: resolverlos antes de migrar.
CREATE UNIQUE INDEX IF NOT EXISTS ux_usuarios_email_lower ON usuarios (lower(email));
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS proforma_id bigint REFERENCES proformas(id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_ventas_proforma ON ventas (proforma_id);
INSERT INTO roles (nombre, created_at, updated_at)
VALUES ('ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('SUPERVISOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('VENDEDOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (nombre) DO NOTHING;
COMMIT;
