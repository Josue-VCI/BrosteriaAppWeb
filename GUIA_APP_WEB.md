# Guía de Revisión Modular y Manual de Producción: CRM La Brostería 🍗

Esta guía está diseñada para que puedas probar, evaluar y desplegar el sistema de forma controlada, documentando las **mejores prácticas, normas de seguridad y los errores críticos que nunca se deben volver a cometer** en producción.

---

## 📋 SECCIÓN 1: MÓDULOS DEL CRM (CÓMO PROBAR)

### MÓDULO 1: Seguridad, Roles y Accesos (Admin vs Cajero)
1. **Administrador (`admin@brosteria.com`):** Accede al Dashboard (`/dashboard`) y visualiza todos los menús laterales, incluyendo analíticas.
2. **Cajero (`cajero@brosteria.com`):** Es redirigido automáticamente a la pantalla de Pedidos (`/pedidos`) y los accesos a reportes y paneles financieros quedan físicamente ocultos en el menú lateral.
3. **Bloqueo de Ruta (Guard)**: Si un cajero intenta escribir directamente `/dashboard` en el navegador, el sistema bloquea el acceso y lo expulsa a `/pedidos`.

### MÓDULO 2: Tablero Kanban de Pedidos y Alertas
1. **Alertas Sonoras**: Al ingresar un pedido en estado `PENDIENTE`, la pestaña del Kanban reproduce un sonido de notificación ("Ding-Dong").
2. **Flujo de Producción**: Los botones permiten avanzar de *Pendiente* -> *Cocina* -> *Listo para Despachar* -> *Pagado*.
3. **Optimización de Polling**: Si minimizas la pestaña del navegador o cambias de ventana, el polling en segundo plano se detiene para no consumir batería ni datos móviles en la tablet de cocina.

### MÓDULO 3: Registro de Pedidos (WhatsApp e Integridad)
1. **Parser Inteligente**: Permite copiar el bloque de texto del chat de WhatsApp y procesar el nombre, teléfono, productos (ej. *Promo Duo Crujiente*) y correo de forma automática.
2. **Duplicados Tolerados**: Si el cliente usa un teléfono nuevo pero con un correo electrónico que ya existe en el sistema, el backend de forma inteligente limpia el email (lo deja en `null`) para **evitar caídas de clave única (Error 500)** en Supabase.
3. **Campos Vacíos**: Puedes omitir el nombre o teléfono y el sistema creará un cliente genérico `"Anonimo"` con teléfono `"000000000"` para no rechazar la venta.

---

## 🔒 SECCIÓN 2: NORMAS DE SEGURIDAD Y ERRORES A EVITAR

Para mantener el sistema seguro, libre de costos imprevistos y estable para tus clientes, debes respetar las siguientes directivas:

### 1. Nunca guardes contraseñas en Git (Fuga de Credenciales)
> [!CAUTION]
> Cualquier contraseña, secreto JWT o API key que se suba en texto plano a GitHub queda registrada en el historial permanente del repositorio. Aunque la borres en el commit siguiente, cualquier persona con acceso al código puede recuperar los secretos antiguos.
> **La regla**: Usa siempre marcadores o variables de entorno en tus comandos de consola y mantén los archivos `.properties` libres de secretos reales.

### 2. Bloquea las credenciales predeterminadas en producción
* **El riesgo**: Dejar activa la clave de prueba (`BrosteriaCRM2026!`) permite que cualquiera que conozca el código de tu proyecto acceda a los servidores de tus clientes.
* **La solución implementada**: El backend detecta si corre bajo el perfil `prod` (Producción). Si es así, **desactiva por completo el fallback** de contraseñas semilla. Si las variables de entorno `ADMIN_PASSWORD` y `CAJERO_PASSWORD` no están explícitamente configuradas en Cloud Run, **no se creará ni asignará ninguna clave por defecto**, protegiendo el acceso.

### 3. Restringe el CORS a dominios exactos
* **El riesgo**: Habilitar CORS con comodines generalizados (`*`) permite que otras aplicaciones ajenas roben la sesión de tus usuarios o realicen peticiones falsificadas (CSRF).
* **La solución implementada**: CORS se encuentra configurado para aceptar exclusivamente los orígenes autorizados:
  - `http://localhost:4200` y `http://127.0.0.1:4200` (Desarrollo local).
  - `https://brosteria-app-web.vercel.app` (Frontend oficial de Vercel).
  - `https://brosteria.vci.pe` (Tu dominio personalizado final).

### 4. Normativa de Codificación Cero Mojibake (Sin acentos ni ñ)
* **El riesgo**: El uso de tildes (`á`, `é`), eñes (`ñ`) o emojis en la base de datos, en las plantillas HTML o en los comentarios de código suele generar caracteres rotos (`Â¡`, `Ã³`, `ðŸ...`) cuando se abre el proyecto en sistemas operativos con codificación diferente (ANSI/CP1252 vs UTF-8).
* **La regla**: Todos los nombres comerciales de los productos, descripciones, notificaciones de email y comentarios de desarrollo deben escribirse **sin tildes, sin eñes y sin emojis**. 
  - Ejemplo: Usa `Duo` en lugar de `Dúo`.
  - Ejemplo: Usa `contrasena` en lugar de `contraseña`.
  - Ejemplo: Usa `banadas` en lugar de `bañadas`.

---

## 🚀 SECCIÓN 3: GUÍA RÁPIDA DE DESPLIEGUE Y OPERACIÓN

### 1. Despliegue en Google Cloud Run (Backend)
Para evitar que Google Cloud te facture cargos adicionales, ejecuta el despliegue utilizando la siguiente estructura que limita el uso a un **esquema seguro de costo $0 o mínimo**.

Ejecuta este comando en **Google Cloud Shell** (Linux/Bash), reemplazando los campos entre corchetes con tus credenciales rotadas:

```bash
gcloud run deploy brosteria-backend \
  --source . \
  --region us-central1 \
  --allow-unauthenticated \
  --port 8080 \
  --min-instances 0 \
  --max-instances 1 \
  --memory 512Mi \
  --cpu 1 \
  --concurrency 20 \
  --timeout 60 \
  --set-env-vars "SPRING_PROFILES_ACTIVE=prod,DB_URL=jdbc:postgresql://[SUPABASE_HOST]:6543/postgres?sslmode=require,DB_USERNAME=[SUPABASE_USER],DB_PASSWORD=[DB_PASSWORD],MAIL_USERNAME=[TU_GMAIL],MAIL_PASSWORD=[GMAIL_APP_PASSWORD],JWT_SECRET=[JWT_SEGURO_LARGO],ADMIN_PASSWORD=[NUEVA_CLAVE_ADMIN_PROD],CAJERO_PASSWORD=[NUEVA_CLAVE_CAJERO_PROD]"
```

*Explicación de límites de costo*:
* `--min-instances 0`: Si no hay uso, el servidor se apaga. **Costo de inactividad = S/. 0.00**.
* `--max-instances 1`: Evita que Google clone servidores adicionales en picos de tráfico, limitando el consumo al Plan Gratuito.
* `SPRING_PROFILES_ACTIVE=prod`: Activa la seguridad estricta y silencia el logueo de SQL.

### 2. Actualización de Frontend (Vercel)
Vercel está conectado directamente a tu repositorio de GitHub. Cualquier cambio que subas a la rama principal se compilará y desplegará automáticamente sin comandos adicionales.

Para subir tus cambios a GitHub de forma rápida:
```bash
git add .
git commit -m "Mejoras de seguridad y hardening de produccion"
git push origin main
```

### 3. Mantenimiento y Limpieza de Base de Datos (Soporte)
La opción de purgado automático fue eliminada del panel para evitar pérdidas accidentales. Para dar soporte o mantenimiento:
1. Entra a tu consola de **Supabase**.
2. Si requieres limpiar el historial del catálogo de producción de caracteres antiguos o Mojibake, copia y ejecuta en el **SQL Editor** el script de limpieza provisto en:
   👉 [limpiar_supabase.sql](file:///D:/Proyecto_Brosteria/aplicacion_web/backend/limpiar_supabase.sql)
