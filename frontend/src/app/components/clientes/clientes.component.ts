import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WORLD_CUP_TEMPLATE } from './templates';
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
    if (!query) {
      this.clientesFiltrados = [...this.clientes];
    } else {
      this.clientesFiltrados = this.clientes.filter(c => 
        c.name.toLowerCase().includes(query) || 
        c.phone.includes(query) || 
        (c.email && c.email.toLowerCase().includes(query))
      );
    }
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
    if (!this.asuntoCampana || (!this.cuerpoCampana && this.plantillaSeleccionada !== 'mundial')) return;

    this.enviandoCorreo = true;
    const destinatarios = this.clientes
      .map(c => c.email)
      .filter(email => email !== null && email.trim() !== '');

    let htmlContent = '';
    if (this.plantillaSeleccionada === 'mundial') {
      htmlContent = WORLD_CUP_TEMPLATE;
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
