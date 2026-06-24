import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { API_BASE_URL } from '../../config';

@Component({
  selector: 'app-pedidos',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './pedidos.component.html',
  styleUrls: ['./pedidos.component.css']
})
export class PedidosComponent implements OnInit {
  pedidosPendientes: any[] = [];
  pedidosPreparando: any[] = [];
  pedidosEnviados: any[] = [];
  pedidosEntregados: any[] = [];
  columnaActiva = 'PENDIENTE'; // Control para móviles: PENDIENTE, PREPARANDO, ENVIADO

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.cargarTodosLosPedidos();
  }

  cargarTodosLosPedidos() {
    this.http.get<any[]>(`${API_BASE_URL}/api/v1/pedidos`).subscribe({
      next: (data) => {
        this.pedidosPendientes = data.filter(p => p.status === 'PENDIENTE');
        this.pedidosPreparando = data.filter(p => p.status === 'PREPARANDO');
        this.pedidosEnviados = data.filter(p => p.status === 'ENVIADO');
        this.pedidosEntregados = data.filter(p => p.status === 'ENTREGADO');
      },
      error: (err) => console.error('Error al cargar pedidos del kanban', err)
    });
  }

  actualizarEstado(pedidoId: number, nuevoEstado: string) {
    this.http.put(`${API_BASE_URL}/api/v1/pedidos/${pedidoId}/estado?nuevoEstado=${nuevoEstado}`, {}).subscribe({
      next: () => {
        this.cargarTodosLosPedidos();
      },
      error: (err) => console.error('Error al actualizar estado del pedido', err)
    });
  }
}
