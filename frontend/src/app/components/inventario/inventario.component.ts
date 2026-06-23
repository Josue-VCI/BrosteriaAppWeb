import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { API_BASE_URL } from '../../config';

@Component({
  selector: 'app-inventario',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './inventario.component.html',
  styleUrls: ['./inventario.component.css']
})
export class InventarioComponent implements OnInit {
  insumos: any[] = [];
  
  // Modales
  mostrarModalRefill = false;
  mostrarModalCrud = false;
  esEdicion = false;

  // Insumo para recarga
  insumoSeleccionadoId: number | null = null;
  insumoSeleccionadoNombre = '';
  cantidadIngresar = 10.0;

  // Insumo para CRUD
  formInsumo: any = {
    id: null,
    name: '',
    quantity: 0.0,
    unit: 'unidades',
    minimumStock: 5.0
  };

  private apiBaseUrl = `${API_BASE_URL}/api/v1/insumos`;

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.cargarInsumos();
  }

  cargarInsumos() {
    this.http.get<any[]>(this.apiBaseUrl).subscribe({
      next: (data) => this.insumos = data,
      error: (err) => console.error('Error al cargar insumos', err)
    });
  }

  // Refill Modal Lógica
  abrirIngreso(insumo: any) {
    this.insumoSeleccionadoId = insumo.id;
    this.insumoSeleccionadoNombre = insumo.name;
    this.cantidadIngresar = 10.0;
    this.mostrarModalRefill = true;
  }

  cerrarModalRefill() {
    this.mostrarModalRefill = false;
    this.insumoSeleccionadoId = null;
  }

  guardarIngreso() {
    if (this.insumoSeleccionadoId === null || this.cantidadIngresar <= 0) return;
    
    this.http.post(`${this.apiBaseUrl}/${this.insumoSeleccionadoId}/ingreso?cantidad=${this.cantidadIngresar}`, {}).subscribe({
      next: () => {
        this.cargarInsumos();
        this.cerrarModalRefill();
      },
      error: (err) => console.error('Error al registrar ingreso', err)
    });
  }

  // CRUD Modal Lógica
  abrirNuevoInsumo() {
    this.esEdicion = false;
    this.formInsumo = {
      id: null,
      name: '',
      quantity: 0.0,
      unit: 'unidades',
      minimumStock: 5.0
    };
    this.mostrarModalCrud = true;
  }

  abrirEditarInsumo(insumo: any) {
    this.esEdicion = true;
    this.formInsumo = { ...insumo };
    this.mostrarModalCrud = true;
  }

  cerrarModalCrud() {
    this.mostrarModalCrud = false;
  }

  guardarInsumo() {
    if (!this.formInsumo.name || this.formInsumo.quantity < 0 || this.formInsumo.minimumStock < 0) return;

    if (this.esEdicion && this.formInsumo.id) {
      // Modificar
      this.http.put(`${this.apiBaseUrl}/${this.formInsumo.id}`, this.formInsumo).subscribe({
        next: () => {
          this.cargarInsumos();
          this.cerrarModalCrud();
        },
        error: (err) => console.error('Error al actualizar insumo', err)
      });
    } else {
      // Crear nuevo
      this.http.post(this.apiBaseUrl, this.formInsumo).subscribe({
        next: () => {
          this.cargarInsumos();
          this.cerrarModalCrud();
        },
        error: (err) => console.error('Error al crear insumo', err)
      });
    }
  }

  eliminarInsumo(id: number) {
    if (!confirm('¿Está seguro de eliminar este insumo del inventario?')) return;

    this.http.delete(`${this.apiBaseUrl}/${id}`).subscribe({
      next: () => {
        this.cargarInsumos();
      },
      error: (err) => console.error('Error al eliminar insumo', err)
    });
  }
}
