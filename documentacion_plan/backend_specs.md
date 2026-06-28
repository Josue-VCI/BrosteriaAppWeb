# Especificaciones Técnicas del Backend (Spring Boot)

Este documento contiene la especificación detallada y el código listo para usar del backend de la aplicación CRM de **La Brostería**. La IA ejecutora debe crear este proyecto en `D:\Proyecto_Brosteria\aplicacion_web\backend`.

---

## 📦 1. Dependencias (`pom.xml`)

El archivo `pom.xml` debe configurarse para soportar Java 21, Spring Boot 3.x/4.x, seguridad JWT, base de datos PostgreSQL, mapeo DTO, servicios de correo y generación de PDF:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>
    <groupId>com.upc</groupId>
    <artifactId>brosteria</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>Brosteria CRM Backend</name>
    <description>Backend para el CRM de La Brosteria</description>

    <properties>
        <java.version>21</java.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-mail</artifactId>
        </dependency>

        <!-- PostgreSQL Driver -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Lombok para simplificar código -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- ModelMapper para mapeo de DTOs -->
        <dependency>
            <groupId>org.modelmapper</groupId>
            <artifactId>modelmapper</artifactId>
            <version>3.0.0</version>
        </dependency>

        <!-- JWT para Seguridad -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.11.5</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.11.5</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.11.5</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Generación de PDF (OpenPDF - Open Source libre) -->
        <dependency>
            <groupId>com.github.librepdf</groupId>
            <artifactId>openpdf</artifactId>
            <version>1.3.30</version>
        </dependency>

        <!-- Swagger para documentación interactiva -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.2.0</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## ⚙️ 2. Propiedades de la Aplicación (`application.properties`)

Crear en `backend/src/main/resources/application.properties`:

```properties
spring.application.name=BrosteriaCRM
server.port=8081

# Base de Datos PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/db_brosteria_crm
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Inicialización automática con data.sql
spring.sql.init.mode=always
spring.jpa.defer-datasource-initialization=true

# Configuración de Seguridad JWT
jwt.secret=${JWT_SECRET}
jwt.expiration=86400000

# Configuración SMTP de Gmail (Ajustar credenciales de producción)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=labrosteriapremium@gmail.com
spring.mail.password=crea_una_contrasena_de_aplicacion_en_google
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
```

---

## 🗄️ 3. Entidades JPA Clave

### A. UsuarioEntidad.java
Ubicación: `com.upc.brosteria.Entidades.UsuarioEntidad`
```java
package com.upc.brosteria.Entidades;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class UsuarioEntidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private RolEntidad rolEntidad;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
```

### B. InsumoEntidad.java
Ubicación: `com.upc.brosteria.Entidades.InsumoEntidad`
```java
package com.upc.brosteria.Entidades;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "insumos")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class InsumoEntidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double quantity;

    @Column(nullable = false)
    private String unit; // "kg", "unidades", "litros"

    @Column(nullable = false)
    private Double minimumStock;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PreUpdate
    @PrePersist
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

### C. ClienteEntidad.java
Ubicación: `com.upc.brosteria.Entidades.ClienteEntidad`
```java
package com.upc.brosteria.Entidades;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "clientes")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class ClienteEntidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column
    private String address;

    @Column(columnDefinition = "integer default 0")
    private Integer totalOrders = 0;

    @Column(columnDefinition = "double precision default 0.0")
    private Double totalSpent = 0.0;

    @Column(columnDefinition = "integer default 0")
    private Integer points = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
```

---

## 📩 4. Servicio de Gmail (`EmailServicio.java`)

Este servicio se encarga de enviar notificaciones por correo de manera asíncrona:

```java
package com.upc.brosteria.Servicios;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailServicio {

    @Autowired
    private JavaMailSender mailSender;

    @Async
    public void enviarCorreoHTML(String destinatario, String asunto, String contenidoHtml) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(contenidoHtml, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error al enviar email por SMTP a " + destinatario + ": " + e.getMessage());
        }
    }

    public void notificarStockBajo(String insumoNombre, Double actualStock, String unidad) {
        String asunto = "⚠️ ALERTA DE STOCK CRÍTICO: " + insumoNombre.toUpperCase();
        String html = """
            <div style="font-family: Arial, sans-serif; border: 1px solid #FF1744; border-radius: 8px; padding: 20px; max-width: 600px;">
                <h2 style="color: #FF1744; margin-top: 0;">¡Alerta de Inventario en La Brostería!</h2>
                <p>El insumo <strong>%s</strong> ha alcanzado su límite mínimo de seguridad.</p>
                <div style="background-color: #ffebee; border-left: 5px solid #FF1744; padding: 10px; margin: 15px 0;">
                    <strong>Stock Actual:</strong> %s %s
                </div>
                <p>Por favor, realice el pedido de reposición lo antes posible.</p>
                <hr style="border: 0; border-top: 1px solid #eee; margin-top: 20px;">
                <p style="font-size: 12px; color: #888;">CRM La Brostería - Sistema Automático de Notificaciones</p>
            </div>
            """.formatted(insumoNombre, actualStock, unidad);
        
        // Enviar al administrador
        enviarCorreoHTML("labrosteriapremium@gmail.com", asunto, html);
    }
}
```

---

## 📊 5. Lógica de Reportes en PDF (`PdfServicio.java`)

Generación interactiva y limpia de reportes usando OpenPDF:

```java
package com.upc.brosteria.Servicios;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.upc.brosteria.Entidades.PedidoEntidad;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfServicio {

    public byte[] generarReporteVentasPdf(List<PedidoEntidad> pedidos, Double totalVentas) {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Título
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph titulo = new Paragraph("CRM LA BROSTERÍA - REPORTES DE VENTAS", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(20);
            document.add(titulo);

            // Resumen Ejecutivo
            Font fontSub = FontFactory.getFont(FontFactory.HELVETICA, 12);
            document.add(new Paragraph("Total de Pedidos Procesados: " + pedidos.size(), fontSub));
            document.add(new Paragraph("Venta Bruta Total: S/. " + String.format("%.2f", totalVentas), fontSub));
            document.add(new Paragraph("Generado el: " + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")), fontSub));
            document.add(new Paragraph("\n"));

            // Tabla de Ventas
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.0f, 2.5f, 1.5f, 1.5f, 1.5f, 1.5f});

            // Cabeceras de Tabla
            String[] headers = {"ID", "Cliente", "Fecha", "Tipo", "Pago", "Total"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
                table.addCell(cell);
            }

            // Filas
            for (PedidoEntidad ped : pedidos) {
                table.addCell(ped.getId().toString());
                table.addCell(ped.getCustomerName());
                table.addCell(ped.getOrderDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                table.addCell(ped.getType());
                table.addCell(ped.getPaymentMethod());
                table.addCell("S/. " + String.format("%.2f", ped.getTotal()));
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }
}
```

---

## 📈 6. Controlador de Reportes (`ReporteControlador.java`)

Provee datos estructurados para los gráficos de Chart.js y genera la descarga de archivos PDF:

```java
package com.upc.brosteria.Controladores;

import com.upc.brosteria.Entidades.PedidoEntidad;
import com.upc.brosteria.Repositorios.PedidoRepositorio;
import com.lowagie.text.pdf.PdfWriter;
import com.upc.brosteria.Servicios.PdfServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/reportes")
@CrossOrigin(origins = "http://localhost:4200")
public class ReporteControlador {

    @Autowired
    private PedidoRepositorio pedidoRepositorio;

    @Autowired
    private PdfServicio pdfServicio;

    @GetMapping("/resumen")
    public ResponseEntity<Map<String, Object>> obtenerResumenVentas() {
        List<PedidoEntidad> pedidos = pedidoRepositorio.findAll();
        
        Double totalVentas = pedidos.stream()
                .filter(p -> p.getStatus().equals("ENTREGADO"))
                .mapToDouble(PedidoEntidad::getTotal)
                .sum();

        long totalPedidos = pedidos.size();
        long completados = pedidos.stream().filter(p -> p.getStatus().equals("ENTREGADO")).count();
        long cancelados = pedidos.stream().filter(p -> p.getStatus().equals("CANCELADO")).count();

        Map<String, Object> response = new HashMap<>();
        response.put("ventasTotales", totalVentas);
        response.put("totalPedidos", totalPedidos);
        response.put("completados", completados);
        response.put("cancelados", cancelados);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/descargar-pdf")
    public ResponseEntity<byte[]> descargarReportePdf() {
        List<PedidoEntidad> pedidos = pedidoRepositorio.findAll();
        Double totalVentas = pedidos.stream()
                .filter(p -> p.getStatus().equals("ENTREGADO"))
                .mapToDouble(PedidoEntidad::getTotal)
                .sum();

        byte[] pdfBytes = pdfServicio.generarReporteVentasPdf(pedidos, totalVentas);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.builder("attachment").filename("reporte_brosteria.pdf").build());

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
```
