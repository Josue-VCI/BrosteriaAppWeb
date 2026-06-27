import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { API_BASE_URL } from '../../config';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-pedidos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pedidos.component.html',
  styleUrls: ['./pedidos.component.css']
})
export class PedidosComponent implements OnInit, OnDestroy {
  pedidosCocina: any[] = [];
  pedidosDespacho: any[] = [];
  columnaActiva = 'COCINA'; // Control para moviles: COCINA, DESPACHO

  // Polling y Alerta Sonora
  intervalId: any;
  cantidadPendientesAnterior = -1; // Sentinel -1 evita que suene en el primer load
  audioCtx: AudioContext | null = null;

  // Catalogo y Modal de Parser
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
    status: 'PREPARANDO',
    detalles: [] as any[],
    total: 5.0
  };

  constructor(private http: HttpClient, private toastService: ToastService) {}

  ngOnInit() {
    this.cargarTodosLosPedidos();
    this.cargarProductos();

    // Polling automatico cada 10 segundos
    this.intervalId = setInterval(() => {
      this.cargarTodosLosPedidos();
    }, 10000);

    // Escuchar cambios de visibilidad de pestaña para ahorrar llamadas e impedir acumulacion de audio
    document.addEventListener('visibilitychange', this.onVisibilityChange);
  }

  ngOnDestroy() {
    document.removeEventListener('visibilitychange', this.onVisibilityChange);
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
    if (this.audioCtx) {
      this.audioCtx.close().catch(() => {});
    }
  }

  onVisibilityChange = () => {
    if (document.visibilityState === 'visible') {
      this.cargarTodosLosPedidos();
      if (!this.intervalId) {
        this.intervalId = setInterval(() => {
          this.cargarTodosLosPedidos();
        }, 10000);
      }
    } else {
      if (this.intervalId) {
        clearInterval(this.intervalId);
        this.intervalId = null;
      }
    }
  };

  cargarProductos() {
    this.http.get<any[]>(`${API_BASE_URL}/api/v1/productos`).subscribe({
      next: (data) => this.productosCatalogo = data,
      error: (err) => console.error('Error al cargar catalogo de productos', err)
    });
  }

  cargarTodosLosPedidos() {
    this.http.get<any[]>(`${API_BASE_URL}/api/v1/pedidos/activos`).subscribe({
      next: (data) => {
        const nuevosCocina = data.filter(p => p.status === 'PENDIENTE' || p.status === 'PREPARANDO');

        // Si hay nuevos pedidos en cocina comparado con el estado anterior, sonar timbre
        if (this.cantidadPendientesAnterior !== -1 && nuevosCocina.length > this.cantidadPendientesAnterior) {
          this.reproducirSonidoAlerta();
        }
        
        this.cantidadPendientesAnterior = nuevosCocina.length;

        this.pedidosCocina = nuevosCocina;
        this.pedidosDespacho = data.filter(p => p.status === 'ENVIADO');
      },
      error: (err) => console.error('Error al cargar pedidos del kanban', err)
    });
  }

  actualizarEstado(pedidoId: number, nuevoEstado: string) {
    if (nuevoEstado === 'ENTREGADO') {
      const confirmar = confirm('¿Esta seguro de marcar este pedido como Pagado y Entregado? Se archivara del tablero.');
      if (!confirmar) return;
    }

    // Copias del estado actual para rollback en caso de error
    const copiaCocina = [...this.pedidosCocina];
    const copiaDespacho = [...this.pedidosDespacho];
    const copiaCantidadPendientes = this.cantidadPendientesAnterior;

    // Actualizacion optimista de UI
    if (nuevoEstado === 'PREPARANDO') {
      // El pedido se queda en Cocina pero cambia su estado a PREPARANDO
      this.pedidosCocina = this.pedidosCocina.map(p => 
        p.id === pedidoId ? { ...p, status: 'PREPARANDO' } : p
      );
    } else if (nuevoEstado === 'ENVIADO') {
      // Se mueve de Cocina a Despacho
      const pedido = this.pedidosCocina.find(p => p.id === pedidoId);
      if (pedido) {
        this.pedidosCocina = this.pedidosCocina.filter(p => p.id !== pedidoId);
        this.cantidadPendientesAnterior = this.pedidosCocina.length;
        const pedidoActualizado = { ...pedido, status: 'ENVIADO' };
        this.pedidosDespacho = [...this.pedidosDespacho, pedidoActualizado];
      }
    } else if (nuevoEstado === 'ENTREGADO') {
      // Se elimina de Despacho
      this.pedidosDespacho = this.pedidosDespacho.filter(p => p.id !== pedidoId);
    }

    this.http.put(`${API_BASE_URL}/api/v1/pedidos/${pedidoId}/estado?nuevoEstado=${nuevoEstado}`, {}).subscribe({
      next: () => {
        // Cargar en segundo plano para verificar consistencia
        this.cargarTodosLosPedidos();
      },
      error: (err) => {
        console.error('Error al actualizar estado del pedido', err);
        this.toastService.error('No se pudo actualizar el estado del pedido en el servidor. Revirtiendo cambios...');
        // Revertir UI al estado anterior
        this.pedidosCocina = copiaCocina;
        this.pedidosDespacho = copiaDespacho;
        this.cantidadPendientesAnterior = copiaCantidadPendientes;
      }
    });
  }

  // Alerta Sonora Sintetizada mediante Web Audio API (Ding-Dong) - Reutiliza AudioContext
  reproducirSonidoAlerta() {
    try {
      if (!this.audioCtx) {
        const AudioCtxClass = window.AudioContext || (window as any).webkitAudioContext;
        if (!AudioCtxClass) return;
        this.audioCtx = new AudioCtxClass();
      }
      
      if (this.audioCtx.state === 'suspended') {
        this.audioCtx.resume();
      }
      
      // Nota 1: Ding (Do5)
      this.emitirTono(this.audioCtx, 523.25, 0.15, 0);
      // Nota 2: Dong (Mi5)
      this.emitirTono(this.audioCtx, 659.25, 0.35, 0.15);
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

  // Logica del Parser de WhatsApp
  abrirNuevoPedido() {
    this.textoWhatsApp = '';
    this.formPedido = {
      customerName: '',
      customerPhone: '',
      customerAddress: '',
      customerEmail: '',
      distrito: '',
      deliveryCost: 5.0,
      type: 'DELIVERY',
      paymentMethod: 'YAPE',
      status: 'PREPARANDO',
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
    const phoneMatch = text.match(/- Telefono Celular:\s*(.*)/i) || text.match(/- Celular:\s*(.*)/i) || text.match(/- Telefono:\s*(.*)/i);
    const typeMatch = text.match(/- Tipo de entrega.*:\s*(.*)/i);
    const districtMatch = text.match(/- Distrito:\s*(.*)/i);
    const addressMatch = text.match(/- Direccion de entrega.*:\s*(.*)/i);
    const creamsMatch = text.match(/Cremas:\s*(.*)/i) || text.match(/- Cremas.*:\s*(.*)/i) || text.match(/\*Cremas:\*\s*(.*)/i);
    const paymentMatch = text.match(/- Metodo de pago.*:\s*(.*)/i);
    const emailMatch = text.match(/- Correo:\s*(.*)/i) || text.match(/- Email:\s*(.*)/i) || text.match(/Correo:\s*(.*)/i);

    const parsedName = nameMatch ? nameMatch[1].trim() : '';
    const parsedPhone = phoneMatch ? phoneMatch[1].trim() : '';
    const parsedTypeStr = typeMatch ? typeMatch[1].trim().toUpperCase() : 'DELIVERY';
    const parsedDistrict = districtMatch ? districtMatch[1].trim() : '';
    const parsedAddress = addressMatch ? addressMatch[1].trim() : '';
    const parsedCreams = creamsMatch ? creamsMatch[1].trim() : '';
    const parsedPaymentStr = paymentMatch ? paymentMatch[1].trim().toUpperCase() : 'EFECTIVO';
    const parsedEmail = emailMatch ? emailMatch[1].trim() : '';

    const isPickup = parsedTypeStr.includes('RECOJO') || parsedTypeStr.includes('PICKUP') || parsedTypeStr.includes('LOCAL');
    const type = isPickup ? 'PICKUP' : 'DELIVERY';
    const deliveryCost = isPickup ? 0.00 : 5.00;

    let paymentMethod = 'EFECTIVO';
    if (parsedPaymentStr.includes('YAPE')) paymentMethod = 'YAPE';
    else if (parsedPaymentStr.includes('PLIN')) paymentMethod = 'PLIN';
    else if (parsedPaymentStr.includes('TARJETA')) paymentMethod = 'TARJETA';

    // Procesar lineas de productos
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

      // Detectar lineas de productos con mayor flexibilidad (soporta viñetas de guion, asterisco, numeros o directo)
      const isProductLine = cleanLine.match(/^[-\*\d\.\s]*\d+x\s*/i);
      if (inDetail && isProductLine) {
        const match = cleanLine.match(/^[-\*\d\.\s]*(\d+)x\s*([^(]+)/i);
        if (match) {
          const quantity = parseInt(match[1], 10);
          let productName = match[2].trim();

          productName = this.limpiarNombreProducto(productName);

          // Extraer cremas especificas de la fila si estan indicadas entre parentesis (ej: (Cremas: Mayo, Ketchup))
          let creamsDeFila = parsedCreams;
          const creamsInLineMatch = cleanLine.match(/\(Cremas:\s*([^)]+)\)/i);
          if (creamsInLineMatch) {
            creamsDeFila = creamsInLineMatch[1].trim();
          }

          // Buscar coincidencia ultra-robusta en el catalogo
          const product = this.buscarCoincidencia(productName);

          if (product) {
            detalles.push({
              productoId: product.id,
              productoName: product.name,
              productoPrice: product.price,
              quantity: quantity,
              subtotal: quantity * product.price,
              creams: creamsDeFila
            });
          } else {
            // Producto no emparejado: se asume precio 0 y se marca para resolucion manual
            detalles.push({
              productoId: null,
              productoName: productName,
              productoPrice: 0.0,
              quantity: quantity,
              subtotal: 0.0,
              creams: creamsDeFila,
              noCoincide: true
            });
          }
        }
      }
    }

    this.formPedido = {
      customerName: parsedName,
      customerPhone: parsedPhone,
      customerAddress: parsedAddress,
      customerEmail: parsedEmail,
      distrito: parsedDistrict,
      deliveryCost: deliveryCost,
      type: type,
      paymentMethod: paymentMethod,
      status: 'PREPARANDO',
      detalles: detalles,
      total: 0.0
    };

    this.recalcularTotal();
  }

  limpiarNombreProducto(name: string): string {
    return name
      .replace(/[\uE000-\uF8FF]|\uD83C[\uDC00-\uDFFF]|\uD83D[\uDC00-\uDFFF]|[\u2011-\u26FF]|\uD83E[\uDD10-\uDDFF]/g, '')
      .replace(/[⚽🏆🥅⏱️🤤🍗🦴🍟🍔🌶️😎🎉]/g, '')
      .replace(/[^\w\s\d().,aeiouAEIOUñÑ-]/g, '')
      .trim();
  }

  normalizarParaComparar(text: string): string {
    if (!text) return '';
    return text
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '') // Quitar tildes y diacriticos
      .toLowerCase()
      .replace(/[^\w\s]/g, '') // Eliminar caracteres especiales
      .replace(/\s+/g, ' ') // Colapsar espacios multiples
      .trim();
  }

  buscarCoincidencia(productName: string): any {
    const cleanInput = this.normalizarParaComparar(productName);
    if (!cleanInput) return null;

    // 1. Coincidencia exacta o contencion directa
    let match = this.productosCatalogo.find(p => {
      const cleanCat = this.normalizarParaComparar(p.name);
      return cleanCat === cleanInput || cleanCat.includes(cleanInput) || cleanInput.includes(cleanCat);
    });
    if (match) return match;

    // 2. Coincidencia por palabras significativas (token matching)
    const inputWords = cleanInput.split(' ').filter(w => w.length > 2);
    if (inputWords.length === 0) return null;

    let mejorProducto = null;
    let maxPalabrasCoincidentes = 0;

    for (const prod of this.productosCatalogo) {
      const cleanCat = this.normalizarParaComparar(prod.name);
      const catWords = cleanCat.split(' ').filter(w => w.length > 2);
      
      const coincidentes = inputWords.filter(w => catWords.includes(w)).length;
      if (coincidentes > maxPalabrasCoincidentes) {
        maxPalabrasCoincidentes = coincidentes;
        mejorProducto = prod;
      }
    }

    // Si coincide al menos el 60% de las palabras significativas, se considera coincidencia
    if (mejorProducto && (maxPalabrasCoincidentes / inputWords.length) >= 0.6) {
      return mejorProducto;
    }

    return null;
  }

  agregarFilaDetalle() {
    this.formPedido.detalles.push({
      productoId: null,
      productoName: '',
      productoPrice: 0.0,
      quantity: 1,
      subtotal: 0.0,
      creams: '',
      noCoincide: true
    });
    this.recalcularTotal();
  }

  eliminarFilaDetalle(index: number) {
    this.formPedido.detalles.splice(index, 1);
    this.recalcularTotal();
  }

  onQuantityChange(detalle: any) {
    if (detalle.quantity < 1) {
      detalle.quantity = 1;
    }
    detalle.subtotal = detalle.quantity * (detalle.productoPrice || 0);
    this.recalcularTotal();
  }

  onProductoChange(detalle: any, event: any) {
    const val = event.target.value;
    if (!val) {
      detalle.productoId = null;
      detalle.productoName = '';
      detalle.productoPrice = 0.0;
      detalle.subtotal = 0.0;
      detalle.noCoincide = true;
      this.recalcularTotal();
      return;
    }
    const prodId = parseInt(val, 10);
    if (isNaN(prodId)) return;

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
    this.formPedido.deliveryCost = this.formPedido.type === 'PICKUP' ? 0.00 : 5.00;
    if (this.formPedido.type === 'PICKUP') {
      this.formPedido.distrito = 'Surquillo';
    }
    const subtotal = this.formPedido.detalles.reduce((sum: number, d: any) => sum + (d.subtotal || 0), 0);
    this.formPedido.total = subtotal + this.formPedido.deliveryCost;
  }

  guardarNuevoPedido() {
    if (this.formPedido.detalles.length === 0) {
      this.toastService.error('Debe agregar al menos un producto al pedido.');
      return;
    }

    const tieneNoCoincidentes = this.formPedido.detalles.some((d: any) => !d.productoId);
    if (tieneNoCoincidentes) {
      this.toastService.warning('Por favor, selecciona un producto valido para todos los elementos.');
      return;
    }

    // Limpiar el payload enviando unicamente los campos esperados por el backend DTO
    const cleanDetalles = this.formPedido.detalles.map((d: any) => ({
      productoId: d.productoId,
      quantity: d.quantity,
      creams: d.creams
    }));

    let finalAddress = this.formPedido.customerAddress;
    if (this.formPedido.type === 'PICKUP') {
      finalAddress = 'Retiro en local';
    } else if (this.formPedido.distrito && finalAddress) {
      if (!finalAddress.toLowerCase().includes(this.formPedido.distrito.toLowerCase())) {
        finalAddress = `${finalAddress}, ${this.formPedido.distrito}`;
      }
    }

    const payload = {
      customerName: this.formPedido.customerName,
      customerPhone: this.formPedido.customerPhone,
      customerAddress: finalAddress,
      customerEmail: this.formPedido.customerEmail,
      deliveryCost: this.formPedido.deliveryCost,
      type: this.formPedido.type,
      paymentMethod: this.formPedido.paymentMethod,
      detalles: cleanDetalles
    };

    this.http.post(`${API_BASE_URL}/api/v1/pedidos`, payload).subscribe({
      next: () => {
        this.cargarTodosLosPedidos();
        this.cerrarModalNuevoPedido();
        this.toastService.success('Pedido registrado con exito.');
      },
      error: (err) => {
        console.error('Error al guardar pedido', err);
        this.toastService.error('Ocurrio un error al registrar el pedido.');
      }
    });
  }

  trackByPedido(index: number, item: any): number {
    return item.id;
  }

  tieneFilasSinProducto(): boolean {
    return this.formPedido.detalles && this.formPedido.detalles.some((d: any) => !d.productoId);
  }

  // Ver pedidos entregados de hoy
  mostrarModalEntregados = false;
  pedidosEntregadosHoy: any[] = [];
  cargandoEntregados = false;

  verPedidosEntregadosHoy() {
    this.cargandoEntregados = true;
    this.mostrarModalEntregados = true;
    this.http.get<any[]>(`${API_BASE_URL}/api/v1/pedidos/estado/ENTREGADO`).subscribe({
      next: (data) => {
        const todayStr = new Date().toDateString();
        this.pedidosEntregadosHoy = data.filter(p => {
          if (!p.orderDate) return false;
          return new Date(p.orderDate).toDateString() === todayStr;
        });
        this.cargandoEntregados = false;
      },
      error: (err) => {
        console.error('Error al cargar entregados de hoy', err);
        this.cargandoEntregados = false;
        this.toastService.error('No se pudieron cargar los pedidos entregados.');
      }
    });
  }

  cerrarModalEntregados() {
    this.mostrarModalEntregados = false;
  }

  setDeliveryType(type: string) {
    this.formPedido.type = type;
    this.recalcularTotal();
  }
}


