# Límites del Plan Gratuito e Infraestructura Low-Cost

Este documento detalla las limitaciones técnicas y operativas del ecosistema gratuito seleccionado para el despliegue del CRM, junto con recomendaciones para mitigarlos sin incurrir en costos fijos.

---

## ☁️ 1. Google Cloud Run (Servidor Backend)

* **El Límite**: Capacidad de escalar a cero instancias inactivas.
* **El Impacto (Arranque en Frío)**: Si la aplicación pasa varias horas sin uso (por ejemplo, durante la mañana cuando el restaurante está cerrado), Google Cloud Run apagará el servidor. El primer mesero que intente registrar un pedido a las 6:00 PM experimentará una demora de **5 a 10 segundos** mientras la máquina virtual vuelve a encenderse.
* **Mitigación ya Implementada**:
  - El frontend cuenta con un indicador de carga ("Conectando al servidor...") para avisar al mesero.
  - El interceptor HTTP de Angular reintentará automáticamente las consultas de lectura en segundo plano hasta 2 veces con 1.5s de delay, absorbiendo esta latencia inicial de forma invisible.
* **Mitigación Avanzada (De Pago)**: Si el cliente no tolera esta latencia inicial, se puede configurar en Cloud Run la opción `--min-instances 1`. Esto mantendrá 1 instancia siempre encendida, con un costo aproximado de **$3 a $5 USD mensuales**.

---

## 🗄️ 2. Supabase (Base de Datos PostgreSQL)

* **El Límite (Free Tier)**:
  - Almacenamiento máximo de base de datos de **500 MB**.
  - Pausa por inactividad de 7 días. Si no hay peticiones a la base de datos en una semana, Supabase la pausará. El siguiente acceso tardará unos 30 segundos en reactivarla.
* **El Impacto**: 500 MB son suficientes para guardar aproximadamente 500,000 pedidos (solo texto). Sin embargo, si el restaurante opera varios años a alto volumen, podría alcanzar el límite.
* **Mitigación**:
  - Descargar periódicamente un respaldo de los reportes en formato **CSV** desde la pantalla de Reportes.
  - Realizar una depuración de pedidos antiguos mediante soporte manual en caso de saturación extrema de espacio.
* **Escalamiento (De Pago)**: El plan **Pro de Supabase ($25 USD al mes)** elimina las pausas automáticas y eleva el almacenamiento a 8 GB.

---

## ✉️ 3. Gmail SMTP (Alertas por Correo)

* **El Límite**: Gmail gratuito permite un máximo de **500 correos enviados por día** por cuenta.
* **El Impacto**: El sistema envía comprobantes de pago automáticos por correo a los clientes al marcar un pedido como "Pagado". Si el restaurante atiende más de 500 pedidos diarios con correo, alcanzará la cuota de Gmail y los correos dejarán de salir.
* **Mitigación**:
  - Presentar la funcionalidad como **"Campañas Básicas y Notificaciones de Comprobantes"**, no como una plataforma de mailing profesional (como Mailchimp).
* **Escalamiento (De Pago)**: Si se requiere enviar boletines masivos o superar los 500 correos diarios, se debe integrar un servicio SMTP comercial como **SendGrid** (que ofrece 100 correos diarios gratis y planes desde $15/mes por 50,000 correos) o **Mailgun**.

---

## 🌐 4. Vercel (Hosting Frontend y Landing)

* **El Límite (Hobby Plan)**:
  - Límite de transferencia de **100 GB mensuales**.
  - Restricción de uso no comercial en términos de servicio.
* **El Impacto**: Si las imágenes cargadas en el menú web público son muy pesadas (ej. fotos de combos de 1 MB), 10,000 visitas al mes consumirán 10 GB de transferencia. Si se llega a 100 GB, Vercel podría suspender la cuenta o requerir actualización.
* **Mitigación**:
  - **Optimización Obligatoria**: Comprimir todas las imágenes en formato **WebP** y reducir su resolución antes de subirlas. Esto bajará el peso de cada foto a menos de 50 KB (reducción del 95% del consumo de ancho de banda).
