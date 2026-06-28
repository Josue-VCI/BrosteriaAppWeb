# Reporte de Auditoría Técnica Completa - Brostería CRM 🍗

Este reporte detalla los hallazgos técnicos, riesgos de seguridad, cuellos de botella de rendimiento y debilidades lógicas identificadas en el estado actual de la base de código. Se categorizan según su impacto para facilitar su priorización.

---

## 🔒 1. Hallazgos de Seguridad

### 🔴 Alertas de Stock Crítico Hardcodeadas (Fuga de Datos Multi-Tenant)
* **Archivo**: [EmailServicio.java:54](file:///D:/Proyecto_Brosteria/aplicacion_web/backend/src/main/java/com/upc/brosteria/Servicios/EmailServicio.java#L54)
* **El Problema**: El correo de destino para alertas de inventario bajo está escrito de forma fija en el código: `"labrosteriapremium@gmail.com"`.
* **El Impacto**: Si este sistema se vende como un software SaaS a otros restaurantes en el futuro, todas las alertas de inventario de todos los clientes llegarán al correo del restaurante original. Esto constituye una fuga de información operativa y rompe la arquitectura multi-inquilino.
* **Solución**: La dirección de alerta debe leerse de la configuración del perfil del inquilino (tenant) en la base de datos o de una variable de entorno.

### 🟡 Almacenamiento de Sesiones Vulnerable a XSS
* **Ubicación**: Frontend (Angular LocalStorage)
* **El Problema**: El token JWT de sesión se guarda directamente en el `localStorage` del navegador.
* **El Impacto**: Si un atacante logra inyectar código JavaScript malicioso en el frontend (ataque XSS) mediante alguna librería de terceros desactualizada o inputs mal sanitizados, podrá leer el token de administrador y robar la sesión completa.
* **Solución**: Para producción estricta, es aconsejable implementar el almacenamiento de tokens mediante cookies con las directivas `HttpOnly`, `Secure` y `SameSite=Strict`, impidiendo el acceso al token desde JavaScript.

### 🟡 Excepciones Internas Enmascaradas pero No Logueadas
* **Archivo**: [GlobalExceptionHandler.java:59](file:///D:/Proyecto_Brosteria/aplicacion_web/backend/src/main/java/com/upc/brosteria/Excepciones/GlobalExceptionHandler.java#L59)
* **El Problema**: El controlador de excepciones captura cualquier error no controlado (`Exception.class`) y devuelve una respuesta segura sin detalles técnicos. Sin embargo, no hace ningún log del error original.
* **El Impacto**: Si ocurre un error 500 inesperado en producción, el cliente verá un mensaje genérico y el administrador de Cloud Run no verá ningún detalle de la falla ni de la pila de llamadas (stacktrace) en la consola de Google Cloud, haciendo imposible el diagnóstico.
* **Solución**: Añadir un objeto `Logger` e imprimir el stacktrace (`log.error(...)`) en el servidor antes de retornar la respuesta enmascarada.

---

## ⚙️ 2. Hallazgos de Lógica y Estabilidad

### 🔴 Spam en Alertas de Inventario
* **Archivo**: [InsumoServicio.java:73](file:///D:/Proyecto_Brosteria/aplicacion_web/backend/src/main/java/com/upc/brosteria/Servicios/InsumoServicio.java#L73) en relación con `PedidoServicio.java:200`
* **El Problema**: Por cada detalle de producto en un pedido, el sistema descuenta stock y comprueba si cayó por debajo del mínimo para enviar una alerta por correo de forma inmediata.
* **El Impacto**: Si un cliente pide un combo que descuenta 3 insumos que ya se encuentran bajo el mínimo, el sistema enviará **3 correos electrónicos consecutivos** de golpe. Si se registran 50 pedidos durante la noche con insumos bajos, el sistema enviará 150 correos, saturando el límite de 500 emails diarios de Gmail SMTP gratis y bloqueando el canal de comunicación.
* **Solución**: Añadir una columna `last_alerted_at` en la tabla de insumos y no enviar una alerta si ya se envió una en las últimas 12 o 24 horas.

### 🟡 Uso de Precisión Doble en Valores Monetarios (Double vs BigDecimal)
* **Ubicación**: Múltiples clases de DTOs, Entidades y Servicios (`PedidoEntidad`, `ProductoEntidad`, `total`, `price`).
* **El Problema**: El sistema maneja variables financieras y de contabilidad usando el tipo primitivo `Double` / `double`.
* **El Impacto**: Los tipos de punto flotante en Java sufren de errores de redondeo binario (IEEE 754). En un volumen alto de pedidos, sumatorias de decimales pueden resultar en discrepancias menores (por ejemplo, S/. 15.0000000004 o S/. 14.99999999) que descuadran reportes fiscales o auditorías contables.
* **Solución**: Reemplazar todos los campos monetarios por el tipo `BigDecimal` en backend y base de datos.

### 🟡 Distrito Fijo e Inmutable en Retiro en Local
* **Archivo**: [pedidos.component.ts:505](file:///D:/Proyecto_Brosteria/aplicacion_web/frontend/src/app/components/pedidos/pedidos.component.ts#L505)
* **El Problema**: Al seleccionar entrega tipo `PICKUP`, el frontend asigna de forma fija el valor `"Surquillo"` al campo de distrito del formulario.
* **El Impacto**: Si el software se despliega para un restaurante ubicado en Miraflores o San Borja, los pedidos archivados como retiro seguirán registrándose bajo el distrito de Surquillo, sesgando los reportes geográficos.
* **Solución**: Hacer que el distrito base del local comercial sea un parámetro configurable en el frontend.

---

## ⚡ 3. Hallazgos de Rendimiento y Escalabilidad

### 🔴 Filtrado de Reportes y Dashboards en Memoria (Cuello de Botella Crítico)
* **Archivo**: [ReporteControlador.java:31](file:///D:/Proyecto_Brosteria/aplicacion_web/backend/src/main/java/com/upc/brosteria/Controladores/ReporteControlador.java#L31)
* **El Problema**: Para obtener resúmenes de ventas filtrados por fecha o canal, el controlador llama a `pedidoRepositorio.findAllWithCliente()`. Esto carga **la totalidad de la tabla de pedidos** de la base de datos a la memoria RAM de Java y luego aplica filtros usando Java Streams.
* **El Impacto**: En un escenario de producción real con 50,000 o 100,000 pedidos históricos:
  1. Cada consulta de reportes o carga del Dashboard descargará megabytes de información inútil de la base de datos Supabase, elevando el consumo de red.
  2. Cloud Run sufrirá picos de uso de CPU y memoria RAM para instanciar miles de objetos Java, gatillando errores de `OutOfMemoryError` y apagando el servidor.
* **Solución**: Escribir consultas SQL en el repositorio que calculen sumas y filtren directamente del lado del motor de base de datos (PostgreSQL), retornando solo la información resumida o el rango exacto de fechas.

### 🟡 Eager Loading (Carga Ansiosa) por Defecto
* **Archivo**: Múltiples entidades (`PedidoEntidad.java`, `DetallePedidoEntidad.java`)
* **El Problema**: Las anotaciones `@ManyToOne` (como `clienteEntidad` o `productoEntidad`) no especifican su estrategia de carga, heredando `FetchType.EAGER` por defecto en JPA.
* **El Impacto**: Cualquier consulta JpaRepository básica (como `findById` sin Join Fetch personalizado) cargará de forma automática y redundante los datos de las tablas relacionadas, multiplicando las llamadas a la base de datos si no se tiene cuidado.
* **Solución**: Configurar todas las relaciones `@ManyToOne` y `@OneToOne` de forma explícita como `FetchType.LAZY` y cargar las relaciones únicamente cuando sea necesario.
