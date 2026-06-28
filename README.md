# Brosteria CRM - Sistema de Gestión y Fidelización SaaS

Este es el repositorio principal del sistema CRM y control de restaurante para **La Brosteria**, diseñado para operar como una plataforma multi-inquilino (SaaS) robusta, de alta velocidad y bajo costo operativo.

---

## 🛠️ Arquitectura Tecnológica Real

La plataforma está dividida en dos componentes desacoplados:

### 1. Backend (Spring Boot 3+)
* **Lenguaje**: Java 17
* **Framework**: Spring Boot 3.x, Spring Security, JPA Hibernate.
* **Seguridad**: Autenticación sin estado (Stateless) mediante JWT tokens y encriptación de contraseñas con `BCrypt`.
* **Base de Datos**: PostgreSQL (alojada en **Supabase** a coste cero), configurada con pool de conexiones robusto mediante **HikariCP** (20 conexiones máximas) y filtrado lógico multi-inquilino (`tenant_id`).
* **Correo**: Notificaciones y comprobantes asíncronos (`@Async`) vía SMTP de Gmail.
* **Alojamiento**: Google Cloud Run (escalable a cero instancias para reducir costos a $0.00 en inactividad).

### 2. Frontend (Angular 17+)
* **Framework**: Angular 17 con arquitectura Standalone Components.
* **Manejo de Red**: Interceptor HTTP con control de timeouts de 12 segundos y reintentos automáticos para tolerar microcaídas de internet o encendido en frío del backend.
* **Tablero Kanban**: Gestión visual interactiva en tiempo real (Cocina / Despacho / Entrega) con alertas sonoras sintetizadas (Web Audio API) y optimización móvil.
* **Alojamiento**: Vercel (Hobby plan de costo cero).

---

## 🗺️ Estructura del Proyecto

* `/backend` - Código fuente del servidor Spring Boot en Java.
* `/frontend` - Código fuente de la interfaz de usuario en Angular.
* `/pagina_web` - Carta pública web y carrito de compras dinámico con enlaces de autogeneración de pedidos de WhatsApp.
