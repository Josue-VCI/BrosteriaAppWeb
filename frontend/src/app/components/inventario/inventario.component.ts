import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { API_BASE_URL } from '../../config';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-inventario',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './inventario.component.html',
  styleUrls: ['./inventario.component.css']
})
export class InventarioComponent implements OnInit {
  insumos: any[] = [];
  esAdmin = false;
  
  // Ordenacion
  columnaOrden = '';
  ordenAscendente = true;

  // Modales
  mostrarModalRefill = false;
  mostrarModalCrud = false;
  esEdicion = false;
  guardandoInsumo = false;
  guardandoIngreso = false;

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

  constructor(private http: HttpClient, private toastService: ToastService) {}

  ngOnInit() {
    this.esAdmin = localStorage.getItem('brosteria_role') === 'ADMIN';
    this.cargarInsumos();
  }

  cargarInsumos() {
    this.http.get<any[]>(this.apiBaseUrl).subscribe({
      next: (data) => {
        this.insumos = data;
        if (this.columnaOrden) {
          this.ordenarPorColumnaActiva();
        } else {
          // Orden por defecto: Criticos primero
          this.insumos.sort((a, b) => {
            const aCritico = a.quantity <= a.minimumStock ? 1 : 0;
            const bCritico = b.quantity <= b.minimumStock ? 1 : 0;
            return bCritico - aCritico;
          });
        }
      },
      error: (err) => console.error('Error al cargar insumos', err)
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
    this.insumos.sort((a, b) => {
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

  // Refill Modal Logica
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
    if (this.insumoSeleccionadoId === null || this.cantidadIngresar <= 0 || this.guardandoIngreso) return;
    
    this.guardandoIngreso = true;
    this.http.post(`${this.apiBaseUrl}/${this.insumoSeleccionadoId}/ingreso?cantidad=${this.cantidadIngresar}`, {}).subscribe({
      next: () => {
        this.guardandoIngreso = false;
        this.toastService.success('Ingreso de stock registrado correctamente');
        this.cargarInsumos();
        this.cerrarModalRefill();
      },
      error: (err) => {
        this.guardandoIngreso = false;
        console.error('Error al registrar ingreso', err);
        this.toastService.error('No se pudo registrar el ingreso de stock');
      }
    });
  }

  // Validaciones del Formulario de Insumos
  esNombreValido(): boolean {
    return !!this.formInsumo.name && this.formInsumo.name.trim().length >= 3;
  }

  esCantidadValida(): boolean {
    return this.formInsumo.quantity !== null && this.formInsumo.quantity !== undefined && this.formInsumo.quantity >= 0;
  }

  esStockMinimoValido(): boolean {
    return this.formInsumo.minimumStock !== null && this.formInsumo.minimumStock !== undefined && this.formInsumo.minimumStock >= 0;
  }

  esFormularioValido(): boolean {
    return this.esNombreValido() && this.esCantidadValida() && this.esStockMinimoValido();
  }

  // CRUD Modal Logica
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
    if (!this.esFormularioValido() || this.guardandoInsumo) return;

    this.guardandoInsumo = true;

    if (this.esEdicion && this.formInsumo.id) {
      // Modificar
      this.http.put(`${this.apiBaseUrl}/${this.formInsumo.id}`, this.formInsumo).subscribe({
        next: () => {
          this.guardandoInsumo = false;
          this.toastService.success('Insumo actualizado correctamente');
          this.cargarInsumos();
          this.cerrarModalCrud();
        },
        error: (err) => {
          this.guardandoInsumo = false;
          console.error('Error al actualizar insumo', err);
          this.toastService.error('No se pudo actualizar el insumo');
        }
      });
    } else {
      // Crear nuevo
      this.http.post(this.apiBaseUrl, this.formInsumo).subscribe({
        next: () => {
          this.guardandoInsumo = false;
          this.toastService.success('Insumo guardado correctamente');
          this.cargarInsumos();
          this.cerrarModalCrud();
        },
        error: (err) => {
          this.guardandoInsumo = false;
          console.error('Error al crear insumo', err);
          this.toastService.error('No se pudo guardar el insumo');
        }
      });
    }
  }

  eliminarInsumo(id: number) {
    if (!confirm('¿Esta seguro de eliminar este insumo del inventario?')) return;

    this.http.delete(`${this.apiBaseUrl}/${id}`).subscribe({
      next: () => {
        this.toastService.success('Insumo eliminado correctamente');
        this.cargarInsumos();
      },
      error: (err) => {
        console.error('Error al eliminar insumo', err);
        this.toastService.error('No se pudo eliminar el insumo');
      }
    });
  }
}
