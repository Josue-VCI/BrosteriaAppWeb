# Especificaciones Técnicas del Frontend (Angular 22 Standalone)

Este documento contiene la especificación de componentes y estilos clave del frontend Angular para **La Brostería**. La IA ejecutora debe crear este proyecto en `D:\Proyecto_Brosteria\aplicacion_web\frontend`.

---

## 🎨 1. Estilos Globales de la Marca (`src/styles.css`)

Importación de tipografías desde Google Fonts, definición del sistema de tokens (colores, sombras, bordes) y base de estilos glassmorphism premium:

```css
@import url('https://fonts.googleapis.com/css2?family=Fredoka:wght@300..700&family=Oswald:wght@200..700&family=Quicksand:wght@300..700&display=swap');

:root {
    --primary: #FF6B00;         /* Naranja Broster */
    --primary-light: #FF8A33;   /* Naranja claro */
    --primary-dark: #CC5500;    /* Naranja oscuro */
    --secondary: #FFB703;       /* Amarillo Fuego */
    --secondary-light: #FFCA3A; /* Amarillo claro */
    --bg-dark: #121214;         /* Fondo oscuro principal */
    --bg-card: rgba(30, 30, 35, 0.65); /* Fondo tarjetas Glassmorphic */
    --bg-body: #0A0A0B;         /* Fondo detrás de tarjetas (near-black) */
    --text-light: #FAFAFA;      /* Blanco principal */
    --text-muted: #A0A0A8;      /* Gris para descripciones */
    --glass-border: rgba(255, 255, 255, 0.06);
    --glass-border-hover: rgba(255, 107, 0, 0.35);
    --success: #00C853;         /* Verde exitoso / Yape */
    --error: #FF1744;           /* Rojo error */
    --font-brand: 'Fredoka', sans-serif;
    --font-heading: 'Oswald', sans-serif;
    --font-body: 'Quicksand', sans-serif;
}

body {
    margin: 0;
    padding: 0;
    background-color: var(--bg-body);
    color: var(--text-light);
    font-family: var(--font-body);
    overflow-x: hidden;
    background-image: radial-gradient(var(--glass-border) 1px, transparent 0);
    background-size: 24px 24px;
}

/* Tarjetas Estilo Glassmorphism Premium */
.glass-card {
    background: var(--bg-card);
    backdrop-filter: blur(16px);
    -webkit-backdrop-filter: blur(16px);
    border: 1px solid var(--glass-border);
    border-radius: 20px;
    box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.37);
    padding: 20px;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.glass-card:hover {
    border-color: var(--glass-border-hover);
    box-shadow: 0 8px 32px 0 rgba(255, 107, 0, 0.15);
    transform: translateY(-4px);
}

/* Títulos y Botones de la Marca */
h1, h2, h3 {
    font-family: var(--font-brand);
    color: var(--text-light);
}

.brand-btn {
    background: linear-gradient(135deg, var(--primary), var(--primary-dark));
    color: white;
    border: none;
    font-family: var(--font-brand);
    font-weight: 600;
    border-radius: 50px;
    padding: 10px 24px;
    cursor: pointer;
    transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.brand-btn:hover {
    transform: scale(1.04);
    box-shadow: 0 0 15px rgba(255, 107, 0, 0.5);
}

.number-font {
    font-family: var(--font-heading);
}
```

---

## ⚙️ 2. Configuración Standalone (`src/app/app.config.ts`)

Configuración de la inyección de dependencias básicas, routing e interceptor JWT:

```typescript
import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { HttpInterceptorFn } from '@angular/common/http';

// Interceptor para inyectar token JWT automáticamente
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('brosteria_token');
  if (token) {
    const cloned = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
    return next(cloned);
  }
  return next(req);
};

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([jwtInterceptor])),
    provideAnimationsAsync()
  ]
};
```

---

## 📊 3. Componente de Reportes (`src/app/components/reportes/`)

Este componente consume las APIs de reporte estadísticas y renderiza los gráficos de Chart.js dinámicamente.

### A. Archivo HTML (`reportes.component.html`)
```html
<div class="reportes-container">
  <div class="header-section">
    <h2>📊 Reportes y Analítica CRM</h2>
    <button class="brand-btn" (click)="descargarReportePdf()">
      📥 Descargar Reporte Completo (PDF)
    </button>
  </div>

  <!-- Panel de Resumen Estadístico -->
  <div class="kpis-grid">
    <div class="glass-card kpi-card">
      <span class="kpi-label">Ingresos Totales (Entregado)</span>
      <h3 class="number-font kpi-value text-success">S/. {{ resumen.ventasTotales | number:'1.2-2' }}</h3>
    </div>
    <div class="glass-card kpi-card">
      <span class="kpi-label">Total Pedidos Procesados</span>
      <h3 class="number-font kpi-value">{{ resumen.totalPedidos }}</h3>
    </div>
    <div class="glass-card kpi-card">
      <span class="kpi-label">Pedidos Completados</span>
      <h3 class="number-font kpi-value text-success">{{ resumen.completados }}</h3>
    </div>
    <div class="glass-card kpi-card">
      <span class="kpi-label">Pedidos Cancelados</span>
      <h3 class="number-font kpi-value text-error">{{ resumen.cancelados }}</h3>
    </div>
  </div>

  <!-- Sección de Gráficos -->
  <div class="charts-grid">
    <div class="glass-card chart-wrapper">
      <h4>Tendencia de Ventas (Histórico)</h4>
      <canvas id="ventasChart"></canvas>
    </div>
    <div class="glass-card chart-wrapper">
      <h4>Métodos de Pago más Utilizados</h4>
      <canvas id="pagosChart"></canvas>
    </div>
  </div>
</div>
```

### B. Archivo TypeScript (`reportes.component.ts`)
```typescript
import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Chart, registerables } from 'chart.js';
import { CommonModule } from '@angular/common';

Chart.register(...registerables);

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './reportes.component.html',
  styleUrls: ['./reportes.component.css']
})
export class ReportesComponent implements OnInit {
  resumen: any = { ventasTotales: 0, totalPedidos: 0, completados: 0, cancelados: 0 };
  private apiBaseUrl = 'http://localhost:8081/api/v1/reportes';

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.cargarResumen();
    this.cargarGraficos();
  }

  cargarResumen() {
    this.http.get(`${this.apiBaseUrl}/resumen`).subscribe({
      next: (data) => this.resumen = data,
      error: (err) => console.error('Error al cargar KPIs de ventas', err)
    });
  }

  cargarGraficos() {
    // Gráfico de Ventas Histórico (Datos dinámicos / simulados desde el backend)
    new Chart('ventasChart', {
      type: 'line',
      data: {
        labels: ['Semana 1', 'Semana 2', 'Semana 3', 'Semana 4', 'Semana 5', 'Semana 6', 'Semana 7', 'Semana 8'],
        datasets: [{
          label: 'Ventas Semanales (S/.)',
          data: [1200, 1900, 3200, 2800, 3900, 4800, 4200, 5600], // Simulado
          borderColor: '#FF6B00',
          backgroundColor: 'rgba(255, 107, 0, 0.15)',
          fill: true,
          tension: 0.4
        }]
      },
      options: {
        responsive: true,
        plugins: { legend: { display: false } }
      }
    });

    // Gráfico de Métodos de Pago
    new Chart('pagosChart', {
      type: 'doughnut',
      data: {
        labels: ['Yape', 'Plin', 'Efectivo'],
        datasets: [{
          data: [55, 25, 20], // Porcentajes simulados
          backgroundColor: ['#00C853', '#00B0FF', '#FFB703'],
          borderWidth: 0
        }]
      },
      options: {
        responsive: true
      }
    });
  }

  descargarReportePdf() {
    this.http.get(`${this.apiBaseUrl}/descargar-pdf`, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Reporte_Brosteria_${new Date().toISOString().slice(0,10)}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => console.error('Error al descargar el PDF del reporte', err)
    });
  }
}
```

---

## 🍗 4. Componente de Inventario (`src/app/components/inventario/`)

Administración de ingredientes y control de niveles críticos.

### A. Archivo HTML (`inventario.component.html`)
```html
<div class="inventario-container">
  <div class="header-section">
    <h2>🍗 Control de Inventario e Insumos</h2>
    <button class="brand-btn" (click)="abrirModalIngreso()">➕ Registrar Compra/Ingreso</button>
  </div>

  <div class="glass-card table-wrapper">
    <table class="inventario-table">
      <thead>
        <tr>
          <th>Insumo</th>
          <th>Cantidad Disponible</th>
          <th>Unidad</th>
          <th>Stock Mínimo</th>
          <th>Estado</th>
          <th>Última Actualización</th>
        </tr>
      </thead>
      <tbody>
        <tr *ngFor="let insumo of insumos" [class.alert-row]="insumo.quantity <= insumo.minimumStock">
          <td><strong>{{ insumo.name }}</strong></td>
          <td class="number-font">{{ insumo.quantity }}</td>
          <td>{{ insumo.unit }}</td>
          <td class="number-font">{{ insumo.minimumStock }}</td>
          <td>
            <span class="badge" [class.badge-success]="insumo.quantity > insumo.minimumStock" [class.badge-danger]="insumo.quantity <= insumo.minimumStock">
              {{ insumo.quantity > insumo.minimumStock ? 'Óptimo' : 'Crítico ⚠️' }}
            </span>
          </td>
          <td>{{ insumo.updatedAt | date:'dd/MM/yyyy HH:mm' }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</div>
```

### B. Archivo TypeScript (`inventario.component.ts`)
```typescript
import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-inventario',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './inventario.component.html',
  styleUrls: ['./inventario.component.css']
})
export class InventarioComponent implements OnInit {
  insumos: any[] = [];
  private apiBaseUrl = 'http://localhost:8081/api/v1/insumos';

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.cargarInsumos();
  }

  cargarInsumos() {
    this.http.get<any[]>(this.apiBaseUrl).subscribe({
      next: (data) => this.insumos = data,
      error: (err) => console.error('Error al cargar insumos', err)
    });
  }

  abrirModalIngreso() {
    // Lógica para abrir modal interactivo para sumar stock de insumos
    // Hace POST a http://localhost:8081/api/v1/insumos/{id}/ingreso con la cantidad
  }
}
```
