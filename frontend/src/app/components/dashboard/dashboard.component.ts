import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { API_BASE_URL } from '../../config';

@Component({
    selector: 'app-dashboard',
    imports: [CommonModule, RouterModule],
    templateUrl: './dashboard.component.html',
    styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  resumen: any = { ventasTotales: 0, totalPedidos: 0, completados: 0, cancelados: 0 };
  insumosCriticos: any[] = [];
  pedidosRecientes: any[] = [];

  // Ordenacion
  columnaOrden = '';
  ordenAscendente = true;

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.cargarResumen();
    this.cargarInventarioCritico();
    this.cargarPedidosRecientes();
  }

  cargarResumen() {
    this.http.get(`${API_BASE_URL}/api/v1/reportes/resumen`).subscribe({
      next: (data) => this.resumen = data,
      error: (err) => console.error('Error al cargar KPIs del dashboard', err)
    });
  }

  cargarInventarioCritico() {
    this.http.get<any[]>(`${API_BASE_URL}/api/v1/insumos`).subscribe({
      next: (data) => {
        // Filtrar insumos criticos
        this.insumosCriticos = data.filter(i => i.quantity <= i.minimumStock);
      },
      error: (err) => console.error('Error al cargar insumos', err)
    });
  }

  cargarPedidosRecientes() {
    this.http.get<any[]>(`${API_BASE_URL}/api/v1/pedidos/recientes?limite=5`).subscribe({
      next: (data) => {
        // Guardar la lista completa ordenada por fecha y limpiar nombres
        this.pedidosRecientes = data
          .map(p => {
            if (p.customerName) {
              p.customerName = p.customerName.replace(/\s*\(\d+\)\s*$/, '');
            }
            return p;
          })
          .sort((a, b) => new Date(b.orderDate).getTime() - new Date(a.orderDate).getTime())
          .slice(0, 5);

        if (this.columnaOrden) {
          this.ordenarPorColumnaActiva();
        }
      },
      error: (err) => console.error('Error al cargar pedidos recientes', err)
    });
  }

  ordenarPor(columna: string) {
    if (this.columnaOrden === columna) {
      this.ordenAscendente = !this.ordenAscendente;
    } else {
      this.columnaOrden = columna;
      this.ordenAscendente = true;
    }
    this.ordenarPorColumnaActiva();
  }

  ordenarPorColumnaActiva() {
    const col = this.columnaOrden;
    this.pedidosRecientes.sort((a, b) => {
      let valA = a[col];
      let valB = b[col];

      if (valA === undefined || valA === null) valA = '';
      if (valB === undefined || valB === null) valB = '';

      if (typeof valA === 'string') {
        return this.ordenAscendente 
          ? valA.localeCompare(valB) 
          : valB.localeCompare(valA);
      } else {
        return this.ordenAscendente 
          ? (valA - valB) 
          : (valB - valA);
      }
    });
  }
}
