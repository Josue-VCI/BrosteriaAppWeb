# Plan y Especificaciones de Desarrollo: CRM La Brostería

Este directorio contiene las especificaciones técnicas completas y el código base ultra-detallado para construir la aplicación web operativa y de CRM de **La Brostería** en la carpeta principal `D:\Proyecto_Brosteria\aplicacion_web`.

---

## 🗂️ Estructura de la Documentación del Plan

1. **[Guía Principal (Este Archivo)](file:///D:/Proyecto_Brosteria/aplicacion_web/documentacion_plan/README.md):** Arquitectura general, estructura de directorios y guía de arranque del entorno.
2. **[Especificación del Backend (Spring Boot)](file:///D:/Proyecto_Brosteria/aplicacion_web/documentacion_plan/backend_specs.md):** Código completo de las 8 Entidades, Configuración de Seguridad JWT, Controladores REST, Servicio de Envío de Correos (Gmail) y Reportes PDF.
3. **[Especificación del Frontend (Angular v22)](file:///D:/Proyecto_Brosteria/aplicacion_web/documentacion_plan/frontend_specs.md):** Configuración Standalone, Estilos con la paleta de colores de La Brostería (Glassmorphism oscuro), e implementación de componentes (Dashboard, Kanban de Pedidos, Inventario y Reportes con Chart.js).
4. **[Script de Base de Datos y Semilla (100 Pedidos SQL)](file:///D:/Proyecto_Brosteria/aplicacion_web/documentacion_plan/db_seed.sql):** Script SQL estructurado compatible con PostgreSQL que simula matemáticamente 100 pedidos e inserta el catálogo y los insumos reales.

---

## ⚙️ Arquitectura Tecnológica

La aplicación web funciona como un CRM y panel de control en tiempo real:

```
+------------------------------------+
|         Angular Frontend           |  <--- Paleta: Naranja Broster, Amarillo Fuego, Dark Glassmorphic
|         (Puerto: 4200)             |  <--- Reportes interactivos con Chart.js
+------------------------------------+
                 |
                 | (Peticiones HTTPS con JWT Interceptor)
                 v
+------------------------------------+
|        Spring Boot Backend         |  <--- Java 21, Spring Security + JWT
|         (Puerto: 8081)             |  <--- JavaMailSender (Notificaciones SMTP Gmail)
+------------------------------------+  <--- OpenPDF (Generación de Reportes PDF)
                 |
                 | (Spring Data JPA)
                 v
+------------------------------------+
|       PostgreSQL Database          |  <--- db_brosteria_crm
|         (Puerto: 5432)             |  <--- Semilla automática con 100+ pedidos históricos
+------------------------------------+
```

---

## 📂 Estructura de Directorios a Crear

La IA ejecutora debe crear la siguiente estructura física de directorios:

```text
D:\Proyecto_Brosteria\aplicacion_web\
│
├── backend/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       └── main/
│           ├── java/com/upc/brosteria/
│           │   ├── Configuracion/
│           │   ├── Controladores/
│           │   ├── DTOs/
│           │   ├── Entidades/
│           │   ├── Excepciones/
│           │   ├── Repositorios/
│           │   ├── Seguridad/
│           │   └── Servicios/
│           └── resources/
│               ├── application.properties
│               └── data.sql
│
├── frontend/
│   ├── package.json
│   ├── tsconfig.json
│   ├── angular.json
│   └── src/
│       ├── index.html
│       ├── main.ts
│       ├── styles.css
│       └── app/
│           ├── app.config.ts
│           ├── app.routes.ts
│           ├── app.ts (Componente Raíz)
│           ├── app.html
│           ├── app.css
│           ├── components/
│           │   ├── login/
│           │   ├── dashboard/
│           │   ├── pedidos/
│           │   ├── clientes/
│           │   ├── inventario/
│           │   └── reportes/
│           ├── services/
│           ├── guards/
│           └── models/
│
└── docker-compose.yaml
```

---

## 🛠️ Guía de Ejecución y Despliegue Rápido

### 1. Inicialización de la Base de Datos con Docker
Se incluye un archivo `docker-compose.yaml` en la raíz del proyecto para levantar PostgreSQL y pgAdmin de inmediato:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: brosteria_db
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: db_brosteria_crm
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    volumes:
      - postgres_data:/var/lib/postgresql/data
    restart: always

  pgadmin:
    image: dpage/pgadmin4
    container_name: brosteria_pgadmin
    ports:
      - "5050:80"
    environment:
      PGADMIN_DEFAULT_EMAIL: admin@brosteria.com
      PGADMIN_DEFAULT_PASSWORD: admin
    depends_on:
      - postgres
    restart: always

volumes:
  postgres_data:
```

### 2. Instrucciones para levantar el Backend
1. Ir a la carpeta `backend/`.
2. Compilar y descargar dependencias de Maven:
   ```bash
   mvn clean install
   ```
3. Ejecutar la aplicación en modo desarrollo:
   ```bash
   mvn spring-boot:run
   ```
4. El backend estará disponible en `http://localhost:8081` y la documentación de la API en `http://localhost:8081/swagger-ui/index.html`.

### 3. Instrucciones para levantar el Frontend
1. Ir a la carpeta `frontend/`.
2. Instalar dependencias de NPM:
   ```bash
   npm install
   ```
3. Ejecutar el servidor de desarrollo:
   ```bash
   npm start
   ```
4. Abrir en el navegador `http://localhost:4200`.

---

## 🚦 Pasos Siguientes para la IA Ejecutora
1. Leer **[backend_specs.md](file:///D:/Proyecto_Brosteria/aplicacion_web/documentacion_plan/backend_specs.md)** para generar todo el código de Spring Boot.
2. Leer **[frontend_specs.md](file:///D:/Proyecto_Brosteria/aplicacion_web/documentacion_plan/frontend_specs.md)** para generar la interfaz de Angular.
3. Copiar el contenido de **[db_seed.sql](file:///D:/Proyecto_Brosteria/aplicacion_web/documentacion_plan/db_seed.sql)** en el archivo `backend/src/main/resources/data.sql` para habilitar los datos simulados de inmediato.
