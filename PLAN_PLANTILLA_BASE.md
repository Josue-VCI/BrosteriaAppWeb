# Plan de Extracción: Creación de Plantilla Base (Boilerplate) 🚀
Este documento detalla el paso a paso técnico para extraer un **Starter Kit (Angular + Spring Boot + Supabase + JWT)** limpio a partir del código actual del CRM de La Brostería, eliminando la lógica de negocio del restaurante para que puedas reutilizarlo en cualquier proyecto futuro.

---

## 📂 FASE 1: Limpieza de Base de Datos y Semilla SQL

Para que la plantilla inicie limpia pero con estructura de usuarios y seguridad funcional:

1. **Modificar [data.sql](file:///D:/Proyecto_Brosteria/aplicacion_web/backend/src/main/resources/data.sql):**
   - **Conservar:**
     - Inserción de roles básicos (`ADMIN`, `USER`, `CAJERO` u otros genéricos).
     - Inserción de usuarios semilla con contraseñas BCrypt (ej: `admin@starter.com` y `cajero@starter.com`).
   - **Eliminar:**
     - Todos los inserts de la tabla `productos`, `insumos`, `clientes`, `pedidos` y `detalle_pedidos`.
     - Toda la simulación matemática masiva de pedidos.
     - Ajustar los reseteos de secuencias (`setval`) solo para las tablas de roles y usuarios.

---

## 💻 FASE 2: Depuración de Backend (Spring Boot)

El objetivo es conservar la arquitectura multicapa (Controlador-Servicio-Entidad-Repositorio) y la configuración de seguridad, removiendo las clases del restaurante.

1. **Eliminar Entidades Específicas:**
   - Borrar del paquete `Entidades`: `ClienteEntidad.java`, `ProductoEntidad.java`, `InsumoEntidad.java`, `PedidoEntidad.java` y `DetallePedidoEntidad.java`.
2. **Eliminar DTOs, Repositorios y Servicios Relacionados:**
   - Eliminar del paquete `DTOs`: `ClienteDTO.java`, `ProductoDTO.java`, `InsumoDTO.java`, `PedidoDTO.java`, `DetallePedidoDTO.java`.
   - Eliminar del paquete `Repositorios`: `ClienteRepositorio.java`, `ProductoRepositorio.java`, `PedidoRepositorio.java`, `DetallePedidoRepositorio.java`, `InsumoRepositorio.java`.
   - Eliminar del paquete `Servicios`: `ClienteServicio.java`, `PedidoServicio.java`, `InsumoServicio.java`, `PdfServicio.java` (remover dependencia de JasperReports si no se requiere generar PDFs de inmediato).
3. **Conservar el Núcleo de Seguridad y Configuración:**
   - **Mantener:**
     - Paquete `Seguridad` (Filtro JWT, Servicio de Detalle de Usuarios, JWT util, Encriptador).
     - Paquete `Configuracion` (Filtro de CORS habilitado para Vercel y localhost, bean de ModelMapper).
     - Entidad `UsuarioEntidad.java` y `RolEntidad.java` con sus respectivos repositorios y controladores de autenticación.
4. **Crear un CRUD de Referencia (Opcional pero Recomendado):**
   - Implementar una entidad simple llamada `ItemEntidad` (con `id`, `name`, `description`, `active`) y sus respectivas capas (DTO, Repositorio, Servicio y Controlador) para que sirva de ejemplo práctico sobre cómo escribir endpoints protegidos por JWT.

---

## 🎨 FASE 3: Depuración del Frontend (Angular Standalone)

Conservaremos el sistema de login y la barra lateral de navegación interactiva y responsiva.

1. **Eliminar Componentes del Dominio:**
   - Borrar las carpetas de componentes: `/pedidos`, `/inventario`, `/clientes` y `/reportes`.
2. **Conservar e Implementar el Esqueleto Principal:**
   - **Mantener:**
     - Componente `/login` con su estilo CSS y consumo del endpoint de login.
     - Guard `auth.guard.ts` (con la redirección y control de roles dinámicos).
     - Archivo `config.ts` (para cambiar fácilmente la URL del backend).
     - Archivos principales `app.ts`, `app.html` y `app.css` (para el Sidebar, toggle móvil y el Logout).
3. **Actualizar el Menú del Sidebar ([app.html](file:///D:/Proyecto_Brosteria/aplicacion_web/frontend/src/app/app.html)):**
   - Reemplazar los enlaces del restaurante por rutas genéricas. Por ejemplo:
     ```html
     <a *ngIf="esAdmin" routerLink="/dashboard" routerLinkActive="active" class="menu-item">Dashboard</a>
     <a routerLink="/items" routerLinkActive="active" class="menu-item">Gestión de Items</a>
     ```
4. **Configurar Componente Inicial Limpio:**
   - Crear un componente básico `/dashboard` o `/home` que simplemente muestre una tarjeta de bienvenida al sistema con el nombre del usuario logueado (`localStorage.getItem('brosteria_username')`).
5. **Ajustar el enrutador ([app.routes.ts](file:///D:/Proyecto_Brosteria/aplicacion_web/frontend/src/app/app.routes.ts)):**
   - Apuntar a los nuevos componentes limpios y mantener protegidos los accesos mediante `canActivate: [authGuard]`.

---

## 🚀 FASE 4: Configuración de Entornos y Publicación

1. **Configurar Variables de Entorno en el Servidor / Cloud:**
   - Asegúrate de documentar en un `README.md` las variables de entorno necesarias para arrancar el backend:
     - `DB_URL`: URL de conexión JDBC a PostgreSQL (Supabase).
     - `DB_USERNAME`: Usuario de base de datos.
     - `DB_PASSWORD`: Contraseña de base de datos.
     - `JWT_SECRET`: Firma para generar los tokens JWT.
2. **Inicializar y Publicar en GitHub:**
   - Crea un nuevo repositorio en GitHub llamado por ejemplo `starter-kit-spring-angular`.
   - Copia los directorios depurados a una nueva carpeta local y ejecuta:
     ```bash
     git init
     git add .
     git commit -m "Initial commit - Starter Kit Angular & Spring Boot"
     git branch -M main
     git remote add origin https://github.com/TU-USUARIO/starter-kit-spring-angular.git
     git push -u origin main
     ```

¡Siguiendo estos pasos, tendrás un Boilerplate listo en menos de 2 horas para desplegar cualquier desarrollo SaaS o CRM en el futuro!
