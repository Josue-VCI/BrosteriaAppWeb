import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  resumen: any = { ventasTotales: 0, totalPedidos: 0, completados: 0, cancelados: 0 };
  insumosCriticos: any[] = [];
  pedidosRecientes: any[] = [];

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.cargarResumen();
    this.cargarInventarioCritico();
    this.cargarPedidosRecientes();
  }

  cargarResumen() {
    this.http.get('http://localhost:8081/api/v1/reportes/resumen').subscribe({
      next: (data) => this.resumen = data,
      error: (err) => console.error('Error al cargar KPIs del dashboard', err)
    });
  }

  cargarInventarioCritico() {
    this.http.get<any[]>('http://localhost:8081/api/v1/insumos').subscribe({
      next: (data) => {
        // Filtrar insumos críticos
        this.insumosCriticos = data.filter(i => i.quantity <= i.minimumStock);
      },
      error: (err) => console.error('Error al cargar insumos', err)
    });
  }

  cargarPedidosRecientes() {
    this.http.get<any[]>('http://localhost:8081/api/v1/pedidos').subscribe({
      next: (data) => {
        // Mostrar los 5 más recientes
        this.pedidosRecientes = data
          .sort((a, b) => new Date(b.orderDate).getTime() - new Date(a.orderDate).getTime())
          .slice(0, 5);
      },
      error: (err) => console.error('Error al cargar pedidos recientes', err)
    });
  }
}
