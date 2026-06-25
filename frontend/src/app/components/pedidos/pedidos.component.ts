import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { API_BASE_URL } from '../../config';

@Component({
  selector: 'app-pedidos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pedidos.component.html',
  styleUrls: ['./pedidos.component.css']
})
export class PedidosComponent implements OnInit, OnDestroy {
  pedidosPendientes: any[] = [];
  pedidosPreparando: any[] = [];
  pedidosEnviados: any[] = [];
  pedidosEntregados: any[] = [];
  columnaActiva = 'PENDIENTE'; // Control para móviles: PENDIENTE, PREPARANDO, ENVIADO

  // Polling y Alerta Sonora
  intervalId: any;
  cantidadPendientesAnterior = 0;

  // Catálogo y Modal de Parser
  productosCatalogo: any[] = [];
  mostrarModalNuevoPedido = false;
  textoWhatsApp = '';
  
  formPedido: any = {
    customerName: '',
    customerPhone: '',
    customerAddress: '',
    deliveryCost: 5.0,
    type: 'DELIVERY',
    paymentMethod: 'YAPE',
    status: 'PENDIENTE',
    detalles: [] as any[],
    total: 5.0
  };

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.cargarTodosLosPedidos();
    this.cargarProductos();

    // Polling automático cada 10 segundos
    this.intervalId = setInterval(() => {
      this.cargarTodosLosPedidos();
    }, 10000);
  }

  ngOnDestroy() {
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
  }

  cargarProductos() {
    this.http.get<any[]>(`${API_BASE_URL}/api/v1/productos`).subscribe({
      next: (data) => this.productosCatalogo = data,
      error: (err) => console.error('Error al cargar catálogo de productos', err)
    });
  }

  cargarTodosLosPedidos() {
    this.http.get<any[]>(`${API_BASE_URL}/api/v1/pedidos/activos`).subscribe({
      next: (data) => {
        const nuevosPendientes = data.filter(p => p.status === 'PENDIENTE');

        // Si hay nuevos pedidos pendientes comparado con el estado anterior, sonar timbre
        if (this.cantidadPendientesAnterior > 0 && nuevosPendientes.length > this.cantidadPendientesAnterior) {
          this.reproducirSonidoAlerta();
        }
        
        this.cantidadPendientesAnterior = nuevosPendientes.length;

        this.pedidosPendientes = nuevosPendientes;
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

  // Alerta Sonora Sintetizada mediante Web Audio API (Ding-Dong)
  reproducirSonidoAlerta() {
    try {
      const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
      if (!AudioCtx) return;
      
      const ctx = new AudioCtx();
      
      // Nota 1: Ding (Do5)
      this.emitirTono(ctx, 523.25, 0.15, 0);
      // Nota 2: Dong (Mi5)
      this.emitirTono(ctx, 659.25, 0.35, 0.15);
    } catch (e) {
      console.warn('No se pudo inicializar o reproducir la alerta sonora de cocina:', e);
    }
  }

  private emitirTono(ctx: AudioContext, frecuencia: number, duracion: number, retraso: number) {
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();

    osc.type = 'sine';
    osc.frequency.setValueAtTime(frecuencia, ctx.currentTime + retraso);

    gain.gain.setValueAtTime(0.3, ctx.currentTime + retraso);
    gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + retraso + duracion);

    osc.connect(gain);
    gain.connect(ctx.destination);

    osc.start(ctx.currentTime + retraso);
    osc.stop(ctx.currentTime + retraso + duracion);
  }

  // Lógica del Parser de WhatsApp
  abrirNuevoPedido() {
    this.textoWhatsApp = '';
    this.formPedido = {
      customerName: '',
      customerPhone: '',
      customerAddress: '',
      deliveryCost: 5.0,
      type: 'DELIVERY',
      paymentMethod: 'YAPE',
      status: 'PENDIENTE',
      detalles: [] as any[],
      total: 5.0
    };
    this.mostrarModalNuevoPedido = true;
  }

  cerrarModalNuevoPedido() {
    this.mostrarModalNuevoPedido = false;
  }

  procesarTextoWhatsApp() {
    const text = this.textoWhatsApp;
    if (!text.trim()) return;

    // Regex para extraer campos estructurados
    const nameMatch = text.match(/- Nombre completo:\s*(.*)/i);
    const typeMatch = text.match(/- Tipo de entrega.*:\s*(.*)/i);
    const addressMatch = text.match(/- Direccion de entrega.*:\s*(.*)/i);
    const creamsMatch = text.match(/- Cremas.*:\s*(.*)/i);
    const paymentMatch = text.match(/- Metodo de pago.*:\s*(.*)/i);

    const parsedName = nameMatch ? nameMatch[1].trim() : '';
    const parsedTypeStr = typeMatch ? typeMatch[1].trim().toUpperCase() : 'DELIVERY';
    const parsedAddress = addressMatch ? addressMatch[1].trim() : '';
    const parsedCreams = creamsMatch ? creamsMatch[1].trim() : '';
    const parsedPaymentStr = paymentMatch ? paymentMatch[1].trim().toUpperCase() : 'EFECTIVO';

    const isPickup = parsedTypeStr.includes('RECOJO') || parsedTypeStr.includes('PICKUP') || parsedTypeStr.includes('LOCAL');
    const type = isPickup ? 'PICKUP' : 'DELIVERY';
    const deliveryCost = isPickup ? 0.00 : 5.00;

    let paymentMethod = 'EFECTIVO';
    if (parsedPaymentStr.includes('YAPE')) paymentMethod = 'YAPE';
    else if (parsedPaymentStr.includes('PLIN')) paymentMethod = 'PLIN';
    else if (parsedPaymentStr.includes('TARJETA')) paymentMethod = 'TARJETA';

    // Procesar líneas de productos
    const detalles: any[] = [];
    const lines = text.split('\n');
    let inDetail = false;

    for (let line of lines) {
      const cleanLine = line.trim();
      if (cleanLine.toLowerCase().includes('detalle del pedido')) {
        inDetail = true;
        continue;
      }
      if (cleanLine.toLowerCase().includes('total de productos') || cleanLine.toLowerCase().includes('total:')) {
        inDetail = false;
      }

      if (inDetail && cleanLine.startsWith('-')) {
        // Formato esperado: "- 2x Combo Pecho Crujiente 🤤 (S/. 26.00)"
        const match = cleanLine.match(/^-\s*(\d+)x\s*([^(]+)/);
        if (match) {
          const quantity = parseInt(match[1], 10);
          let productName = match[2].trim();

          productName = this.limpiarNombreProducto(productName);

          // Buscar coincidencia en catálogo
          const product = this.productosCatalogo.find(p => {
            const cleanCatName = this.limpiarNombreProducto(p.name).toLowerCase();
            const cleanInputName = productName.toLowerCase();
            return cleanCatName.includes(cleanInputName) || cleanInputName.includes(cleanCatName);
          });

          if (product) {
            detalles.push({
              productoId: product.id,
              productoName: product.name,
              productoPrice: product.price,
              quantity: quantity,
              subtotal: quantity * product.price,
              creams: parsedCreams
            });
          } else {
            // Producto no emparejado: se asume precio 0 y se marca para resolución manual
            detalles.push({
              productoId: null,
              productoName: productName,
              productoPrice: 0.0,
              quantity: quantity,
              subtotal: 0.0,
              creams: parsedCreams,
              noCoincide: true
            });
          }
        }
      }
    }

    this.formPedido = {
      customerName: parsedName,
      customerPhone: '', // Se llena a mano
      customerAddress: type === 'PICKUP' ? 'Retiro en local' : parsedAddress,
      deliveryCost: deliveryCost,
      type: type,
      paymentMethod: paymentMethod,
      status: 'PENDIENTE',
      detalles: detalles,
      total: 0.0
    };

    this.recalcularTotal();
  }

  limpiarNombreProducto(name: string): string {
    return name
      .replace(/[\uE000-\uF8FF]|\uD83C[\uDC00-\uDFFF]|\uD83D[\uDC00-\uDFFF]|[\u2011-\u26FF]|\uD83E[\uDD10-\uDDFF]/g, '')
      .replace(/[⚽🏆🥅⏱️🤤🍗🦴🍟🍔🌶️😎🎉]/g, '')
      .trim();
  }

  onProductoChange(detalle: any, event: any) {
    const prodId = parseInt(event.target.value, 10);
    const product = this.productosCatalogo.find(p => p.id === prodId);
    if (product) {
      detalle.productoId = product.id;
      detalle.productoName = product.name;
      detalle.productoPrice = product.price;
      detalle.subtotal = detalle.quantity * product.price;
      detalle.noCoincide = false;
      this.recalcularTotal();
    }
  }

  recalcularTotal() {
    const subtotal = this.formPedido.detalles.reduce((sum: number, d: any) => sum + d.subtotal, 0);
    const delivery = this.formPedido.type === 'PICKUP' ? 0.00 : this.formPedido.deliveryCost;
    this.formPedido.total = subtotal + delivery;
  }

  guardarNuevoPedido() {
    if (!this.formPedido.customerName || this.formPedido.detalles.length === 0) {
      alert('Datos de pedido incompletos.');
      return;
    }

    const tieneNoCoincidentes = this.formPedido.detalles.some((d: any) => !d.productoId);
    if (tieneNoCoincidentes) {
      alert('Por favor, selecciona un producto válido para todos los elementos no coincidentes.');
      return;
    }

    this.http.post(`${API_BASE_URL}/api/v1/pedidos`, this.formPedido).subscribe({
      next: () => {
        this.cargarTodosLosPedidos();
        this.cerrarModalNuevoPedido();
        alert('Pedido registrado con éxito.');
      },
      error: (err) => {
        console.error('Error al guardar pedido', err);
        alert('Ocurrió un error al registrar el pedido.');
      }
    });
  }
}
