# Guía de Revisión Modular y Manual Paso a Paso: CRM La Brostería 🍗

Esta guía está diseñada para que puedas probar, evaluar y validar el sistema **módulo por módulo** de forma independiente. De esta manera, podrás realizar una revisión controlada por bloques sin abrumarte y asegurar que cada sección quede al 100% funcional.

---

## 📋 MÓDULO 1: Seguridad, Roles y Accesos (Admin vs Cajero)
Este bloque regula el control de accesos y la visualización diferenciada según el rol del usuario que inicia sesión.

### Paso a Paso para Evaluar:
1. **Prueba de Acceso de Administrador:**
   - Ve a la pantalla de login e ingresa con:
     - **Email:** `admin@brosteria.com`
     - **Contraseña:** `BrosteriaCRM2026!`
   - **Resultado Esperado:** Debes ser redirigido automáticamente al **Dashboard** (`/dashboard`). En el menú lateral deben verse todos los enlaces, incluyendo *Dashboard* y *Reportes Analíticos*.
2. **Prueba de Acceso de Cajero:**
   - Cierra sesión y ahora inicia con:
     - **Email:** `cajero@brosteria.com`
     - **Contraseña:** `BrosteriaCRM2026!`
   - **Resultado Esperado:** Debes ser redirigido directamente a la pantalla de **Pedidos / Cocina** (`/pedidos`). El enlace de *Dashboard* y el de *Reportes Analíticos* deben estar completamente ocultos en la barra de navegación lateral.
3. **Prueba de Bloqueo por URL (Intrusión):**
   - Estando logueado como Cajero, escribe directamente en la barra de direcciones del navegador: `http://localhost:4200/dashboard` o `http://localhost:4200/reportes` (o la URL de producción).
   - **Resultado Esperado:** El Route Guard (`auth.guard.ts`) interceptará la petición y te redirigirá automáticamente de vuelta a `/pedidos` sin mostrar nada de información restringida.

---

## 📋 MÓDULO 2: Tablero Kanban de Pedidos y Alertas de Cocina
Este bloque gestiona los estados de producción del restaurante: Cocina, Despachado y Archivado (Pagado), con soporte para sonido automático.

### Paso a Paso para Evaluar:
1. **Verificación de Alerta Sonora:**
   - Abre la pestaña del Kanban de Pedidos en tu navegador.
   - Crea o simula un nuevo pedido (usando la base de datos o el modal).
   - **Resultado Esperado:** Al llegar un pedido en estado `PENDIENTE`, el sistema reproducirá una alerta sonora de doble tono sintetizado ("Ding-Dong").
2. **Prueba de Flujo de Estados:**
   - Encuentra un pedido en la columna **Cocina** con estado *Pendiente*. Presiona **"Empezar a Cocinar"**. Su estado debe pasar a *Cocina*.
   - Presiona **"Listo para Despachar"**. El pedido debe moverse a la columna **Despachado** de manera instantánea.
   - En la columna *Despachado*, haz clic en **"Pagado"**. Se te pedirá confirmación. Acepta y el pedido desaparecerá del tablero de pedidos activos (se archiva con estado `ENTREGADO`).
3. **Prueba de Ahorro de Energía/Batería:**
   - Abre el Kanban y minimiza el navegador o cambia a otra pestaña.
   - **Resultado Esperado:** El sistema pausará el polling periódico cada 10 segundos de forma inteligente para no consumir datos ni batería en segundo plano. Al volver a la pestaña, se reactivará.

---

## 📋 MÓDULO 3: Modal de Registro WhatsApp e Inserción Flexible
Este bloque evalúa el parser inteligente de WhatsApp, la adición manual de filas de productos y el comportamiento ante campos vacíos (opcionales).

### Paso a Paso para Evaluar:
1. **Prueba de Pegado e Inteligencia de Match (Promociones):**
   - Presiona el botón **"Registrar Pedido (WhatsApp)"**.
   - Copia y pega en el cuadro de texto raw este mensaje de ejemplo:
     ```text
     NUEVO PEDIDO - LA BROSTERÍA

     Detalle del Pedido:
     - 1x Promo Dúo Crujiente (S/. 22.00)
     - 2x Combo Pecho Crujiente (S/. 26.00)

     Cremas: Mayonesa, Ketchup, Ají de la Casa
     TOTAL A PAGAR: S/. 48.00

     Registro de Promociones:
     - Correo: josuebecerrav19@gmail.com
     ```
   - **Resultado Esperado:** 
     - El parser identificará la **Promo Dúo Crujiente** y el **Combo Pecho Crujiente** de manera automática, colocándoles sus precios e IDs del catálogo.
     - Extraerá el Correo y lo ubicará en su campo.
     - Preguntará primero el *Tipo de Entrega* (antes de la Dirección) y el *Método de Pago* se previsualizará abajo.
2. **Prueba de Inserción Manual (Agregar/Quitar):**
   - En la tabla de productos, presiona **"➕ Agregar Producto"**.
   - **Resultado Esperado:** Se creará una nueva fila en blanco con advertencia en rojo por no estar vinculada.
   - Selecciona `"Mega Balde Brosterero"` en el select dropdown. Verás que la advertencia desaparece, el subtotal cambia a `S/. 69.00` y el total final se incrementa sumando los costos de envío correspondientes.
   - Edita el número de cantidad de `1` a `2`. El subtotal de la fila subirá automáticamente a `S/. 138.00`.
   - Modifica las cremas de esa fila de forma independiente (ej: `"Sin cremas"`).
   - Presiona el icono del tacho de basura `🗑️` al final de cualquier fila. Esa fila se eliminará al instante y el total del pedido se actualizará dinámicamente.
3. **Prueba de Campos Opcionales (Registro Rápido):**
   - Borra por completo el nombre del cliente y el celular (déjalos en blanco).
   - Presiona **"Registrar Pedido"**. (El botón estará habilitado siempre que haya al menos un producto válido seleccionado).
   - **Resultado Esperado:** El pedido se registrará con éxito. En el tablero Kanban verás que el pedido muestra al cliente como `"Anónimo"` y su celular como `"000000000"` (valores robustos autogenerados para respetar la base de datos).

---

## 📋 MÓDULO 4: Reportes Analíticos, Gráficas y Respaldo CSV
Este bloque evalúa el rediseño de las estadísticas de ventas y la exportación de copias de seguridad.

### Paso a Paso para Evaluar:
1. **Prueba de Generación de Reporte PDF:**
   - Inicia sesión como administrador y ve a la sección de **Reportes Analíticos**.
   - Selecciona un rango de fechas (por ejemplo, últimos 30 días) y haz clic en **"Descargar Reporte PDF"**.
   - **Resultado Esperado:** Se descargará un archivo PDF con estilo visual corporativo (Naranja/Negro) que contiene:
     - Métricas clave comparativas contra el periodo equivalente anterior (variación porcentual $\Delta\%$).
     - Tabla ordenada de ingresos según el día de la semana.
     - Gráfico proporcional de barra por bloques horarios (Almuerzo, Tarde, Cena Pico, Cierre).
     - Tabla comparativa de canales (Delivery vs Retiro local).
     - Ranking del Top 5 de Clientes con más consumos.
     *(Nota: Se ha removido por completo el listado antiguo de los últimos 100 pedidos para que el reporte sea conciso y netamente analítico).*
2. **Prueba de Copia de Seguridad CSV:**
   - Presiona el botón **"Descargar Respaldo CSV (90 días)"**.
   - **Resultado Esperado:** Se descargará un archivo con formato `.csv` que contiene el historial totalizado de pedidos del trimestre. Ábrelo en Microsoft Excel y verifica que los caracteres especiales (tildes, eñes y emojis) se rendericen sin fallas gracias al encoding UTF-8 con BOM.

---

## 📋 MÓDULO 5: Inventario y Gestión de Clientes (CRM)
Este bloque regula el descuento automático de suministros e ingredientes por cada venta, así como la acumulación de puntos de fidelización.

### Paso a Paso para Evaluar:
1. **Descuento de Insumos Automático:**
   - Ve a la sección **Inventario** y anota el stock de `"Pollo trozado fresco"` y `"Papas amarillas cortadas"`.
   - Registra un nuevo pedido que contenga un `"1/4 de Pollo Broster"` (el cual requiere 1 pieza de pollo y 0.25 kg de papa).
   - Completa el flujo marcándolo como *Pagado* en el Kanban.
   - Regresa a **Inventario**.
   - **Resultado Esperado:** El stock de los insumos habrá disminuido proporcionalmente de manera automática en el servidor.
2. **Actualización de Clientes y Email:**
   - Ve a la sección **Clientes** y busca si existe un cliente con el teléfono de tu prueba (ej: `960373555`). Anota sus compras acumuladas y puntos.
   - Crea un pedido para ese mismo número telefónico, pero agrégale un correo nuevo (ej: `josue.nuevo@correo.com`).
   - Guarda y procesa el pedido.
   - **Resultado Esperado:** El sistema sumará el monto gastado e incrementará la cantidad de pedidos y puntos del cliente (1 punto por cada S/. 10 gastados). Si el cliente no tenía correo anteriormente, su ficha ahora mostrará el email ingresado.

---

## 🚀 Flujo para Subir y Desplegar Cambios Rápidamente

Cada vez que quieras que implementemos o revises una modificación:

1. **Guardar cambios y subir a la nube:**
   ```bash
   git add .
   git commit -m "Descripción breve del cambio realizado"
   git push origin main
   ```
2. **Reconstruir y desplegar el backend (Cloud Run):**
   *(Ejecuta esto en la terminal donde tengas el Google Cloud SDK)*
   ```bash
   cd ~/BrosteriaAppWeb
   git pull origin main
   cd backend
   gcloud run deploy brosteria-backend \
     --source . \
     --region us-central1 \
     --allow-unauthenticated \
     --set-env-vars DB_URL="jdbc:postgresql://aws-1-us-east-2.pooler.supabase.com:6543/postgres?sslmode=require",DB_USERNAME="postgres.htetwptrtgcwhhpgjfvl",DB_PASSWORD="#16j0sUBV_supabase",MAIL_USERNAME="josuebecerrav19@gmail.com",MAIL_PASSWORD="xudjxokraahmrftp",JWT_SECRET="M1_CL4V3_S3CR3T4_M0Y_L4RG4_Y_S3GUR4_P4R4_3L_PR0Y3CT0_BROSTERIA_CRM_2026"
   ```
3. **Frontend:** Vercel detecta la subida en GitHub de manera automática y redespliega la página en menos de un minuto.
