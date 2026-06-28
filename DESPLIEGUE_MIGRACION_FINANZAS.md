# Despliegue de finanzas y alertas

Este cambio requiere actualizar Supabase antes de desplegar el backend. El orden es obligatorio porque produccion usa `ddl-auto=validate`.

## 1. Actualizar Supabase

1. Abrir Supabase.
2. Entrar a SQL Editor.
3. Copiar y ejecutar `backend/migracion_finanzas_alertas.sql`.
4. Confirmar que la consulta termine sin errores.

El script convierte importes a `NUMERIC`, agrega `last_alerted_at` e instala indices para reportes. Se ejecuta dentro de una transaccion: si una sentencia falla, no deja una migracion parcial.

## 2. Actualizar Cloud Run

Ejecutar en Google Cloud Shell desde el repositorio:

```bash
cd ~/BrosteriaAppWeb
git pull origin main
cd backend

gcloud run deploy brosteria-backend \
  --source . \
  --region us-central1 \
  --allow-unauthenticated \
  --port 8080 \
  --min-instances 0 \
  --max-instances 1 \
  --memory 512Mi \
  --cpu 1 \
  --concurrency 10 \
  --timeout 60 \
  --update-env-vars "STOCK_ALERT_EMAIL=josuebecerrav19@gmail.com,STOCK_ALERT_COOLDOWN_HOURS=12"
```

`--update-env-vars` conserva las credenciales que ya existen en Cloud Run. No usar `--set-env-vars` con solo estas dos variables, porque reemplazaria la configuracion existente.

## 3. Verificar

1. Abrir la revision nueva de Cloud Run y confirmar que esta saludable.
2. Registrar un pedido normal.
3. Abrir Dashboard y Reportes.
4. Llevar un insumo debajo del minimo y confirmar un correo.
5. Repetir otro pedido con ese insumo y confirmar que no llegue otro correo durante 12 horas.

## Reversion

Si el backend presenta un error, dirigir el trafico a la revision anterior desde Cloud Run. No revertir las columnas `NUMERIC`: son compatibles con la version anterior y conservan mejor precision.
