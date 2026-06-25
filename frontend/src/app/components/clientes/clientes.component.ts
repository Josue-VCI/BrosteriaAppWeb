import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WORLD_CUP_TEMPLATE, COMBO_PROMO_TEMPLATE, WEEKEND_PROMO_TEMPLATE } from './templates';
import { API_BASE_URL } from '../../config';

@Component({
  selector: 'app-clientes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './clientes.component.html',
  styleUrls: ['./clientes.component.css']
})
export class ClientesComponent implements OnInit {
  clientes: any[] = [];
  clientesFiltrados: any[] = [];
  busqueda = '';
  
  // Filtros Avanzados
  filtroDistrito = '';
  filtroConsumo = '';
  
  // Ordenamiento
  columnaOrden = '';
  ordenAscendente = true;
  
  // Modales
  mostrarModalCorreo = false;
  mostrarModalCrud = false;
  esEdicion = false;

  // Campaña
  asuntoCampana = '';
  cuerpoCampana = '';
  enviandoCorreo = false;
  plantillaSeleccionada = 'libre';

  onTemplateChange() {
    if (this.plantillaSeleccionada === 'mundial') {
      this.asuntoCampana = '¡GOL DE SABOR MUNDIALISTA! Alienta a la Selección con La Brostería 🍗⚽';
      this.cuerpoCampana = '[Cargada plantilla de correo de promoción mundialista]';
    } else if (this.plantillaSeleccionada === 'combo') {
      this.asuntoCampana = '¡SUPER PROMO 2X1! Duplica el sabor de tus Salchipapas y Pollo 🍟🍗';
      this.cuerpoCampana = '[Cargada plantilla de correo de promoción 2x1]';
    } else if (this.plantillaSeleccionada === 'finsemana') {
      this.asuntoCampana = '🔥 FIN DE SEMANA DE LOCURA: ¡Pollo Broster y Gaseosa Gratis! 🍗🎉';
      this.cuerpoCampana = '[Cargada plantilla de correo de fin de semana]';
    } else {
      this.asuntoCampana = '';
      this.cuerpoCampana = '';
    }
  }

  // Formulario CRUD
  formCliente: any = {
    id: null,
    name: '',
    email: '',
    phone: '',
    address: ''
  };

  private apiBaseUrl = `${API_BASE_URL}/api/v1/clientes`;

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.cargarClientes();
  }

  cargarClientes() {
    this.http.get<any[]>(this.apiBaseUrl).subscribe({
      next: (data) => {
        this.clientes = data;
        this.filtrarClientes();
      },
      error: (err) => console.error('Error al cargar clientes', err)
    });
  }

  filtrarClientes() {
    const query = this.busqueda.toLowerCase().trim();
    let temp = [...this.clientes];

    // 1. Filtrado por búsqueda de texto
    if (query) {
      temp = temp.filter(c => 
        c.name.toLowerCase().includes(query) || 
        c.phone.includes(query) || 
        (c.email && c.email.toLowerCase().includes(query)) ||
        (c.address && c.address.toLowerCase().includes(query))
      );
    }

    // 2. Filtrado por distrito
    if (this.filtroDistrito) {
      temp = temp.filter(c => {
        if (!c.address) return false;
        return c.address.toLowerCase().includes(this.filtroDistrito.toLowerCase());
      });
    }

    // 3. Filtrado por consumo
    if (this.filtroConsumo) {
      temp = temp.filter(c => {
        const spent = c.totalSpent || 0;
        if (this.filtroConsumo === 'alto') return spent > 150;
        if (this.filtroConsumo === 'medio') return spent >= 50 && spent <= 150;
        if (this.filtroConsumo === 'bajo') return spent > 0 && spent < 50;
        if (this.filtroConsumo === 'ninguno') return spent === 0;
        return true;
      });
    }

    this.clientesFiltrados = temp;

    // Re-aplicar ordenación si hay una columna activa
    if (this.columnaOrden) {
      this.ordenarPorColumnaActiva();
    }
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
    this.clientesFiltrados.sort((a, b) => {
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

  // Lógica CRUD
  abrirNuevoCliente() {
    this.esEdicion = false;
    this.formCliente = {
      id: null,
      name: '',
      email: '',
      phone: '',
      address: ''
    };
    this.mostrarModalCrud = true;
  }

  abrirEditarCliente(cliente: any) {
    this.esEdicion = true;
    this.formCliente = { ...cliente };
    this.mostrarModalCrud = true;
  }

  cerrarModalCrud() {
    this.mostrarModalCrud = false;
  }

  guardarCliente() {
    if (!this.formCliente.name || !this.formCliente.phone) return;

    this.http.post(this.apiBaseUrl, this.formCliente).subscribe({
      next: () => {
        this.cargarClientes();
        this.cerrarModalCrud();
      },
      error: (err) => console.error('Error al guardar cliente', err)
    });
  }

  eliminarCliente(id: number) {
    if (!confirm('¿Está seguro de eliminar a este cliente? Se perderá su historial de fidelización.')) return;

    this.http.delete(`${this.apiBaseUrl}/${id}`).subscribe({
      next: () => {
        this.cargarClientes();
      },
      error: (err) => console.error('Error al eliminar cliente', err)
    });
  }

  obtenerDestinatariosValidosCount(): number {
    return this.clientesFiltrados
      .filter(c => c.email && c.email.trim() !== '')
      .length;
  }

  // Lógica Masivos
  abrirRedactarMasivo() {
    this.asuntoCampana = '';
    this.cuerpoCampana = '';
    this.mostrarModalCorreo = true;
  }

  cerrarModalCorreo() {
    this.mostrarModalCorreo = false;
  }

  enviarMasivo() {
    if (!this.asuntoCampana || (!this.cuerpoCampana && this.plantillaSeleccionada === 'libre')) return;

    this.enviandoCorreo = true;
    const destinatarios = this.clientesFiltrados
      .map(c => c.email)
      .filter(email => email !== null && email.trim() !== '');

    let htmlContent = '';
    if (this.plantillaSeleccionada === 'mundial') {
      htmlContent = WORLD_CUP_TEMPLATE;
    } else if (this.plantillaSeleccionada === 'combo') {
      htmlContent = COMBO_PROMO_TEMPLATE;
    } else if (this.plantillaSeleccionada === 'finsemana') {
      htmlContent = WEEKEND_PROMO_TEMPLATE;
    } else {
      htmlContent = `<div style="font-family: Arial, sans-serif; padding: 20px; max-width: 600px; border: 1px solid #FF6B00; border-radius: 8px;">
                     <h2 style="color: #FF6B00;">La Brostería - ¡Promoción Especial!</h2>
                     <p>${this.cuerpoCampana.replace(/\n/g, '<br>')}</p>
                     <hr style="border: 0; border-top: 1px solid #eee; margin-top: 20px;">
                     <p style="font-size: 11px; color: #888;">Recibes este correo porque estás registrado en el club de fidelidad de La Brostería.</p>
                     </div>`;
    }

    const payload = {
      destinatarios: destinatarios,
      asunto: this.asuntoCampana,
      mensajeHtml: htmlContent
    };

    this.http.post(`${this.apiBaseUrl}/enviar-masivo`, payload).subscribe({
      next: () => {
        this.enviandoCorreo = false;
        this.cerrarModalCorreo();
        alert('Campaña de Gmail enviada con éxito a ' + destinatarios.length + ' clientes.');
      },
      error: (err) => {
        this.enviandoCorreo = false;
        console.error('Error al enviar correo masivo', err);
        alert('Ocurrió un error al despachar los correos por SMTP.');
      }
    });
  }
}
