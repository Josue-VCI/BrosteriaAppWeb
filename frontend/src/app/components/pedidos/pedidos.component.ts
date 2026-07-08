import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { API_BASE_URL } from '../../config';
import { ToastService } from '../../services/toast.service';
import { Pedido, Producto } from '../../models/interfaces';
import { Subscription } from 'rxjs';
import { AppLifecycleService } from '../../services/app-lifecycle.service';

@Component({
    selector: 'app-pedidos',
    imports: [CommonModule, FormsModule],
    templateUrl: './pedidos.component.html',
    styleUrls: ['./pedidos.component.css']
})
export class PedidosComponent implements OnInit, OnDestroy {
  pedidosCocina: Pedido[] = [];
  pedidosDespacho: Pedido[] = [];
  private cargandoPedidosActivos = false;
  columnaActiva = 'COCINA'; // Control para moviles: COCINA, DESPACHO

  // Polling y Alerta Sonora
  intervalId: any;
  cantidadPendientesAnterior = -1; // Sentinel -1 evita que suene en el primer load
  audioCtx: AudioContext | null = null;

  // Catalogo y Modal de Parser
  productosCatalogo: Producto[] = [];
  mostrarModalNuevoPedido = false;
  pedidoEditandoId: number | null = null;
  guardandoPedido = false;
  buscandoCliente = false;
  clienteEncontrado = false;
  mostrarDatosCliente = false;
  private telefonoBusquedaTimeout: ReturnType<typeof setTimeout> | null = null;
  private secuenciaBusquedaCliente = 0;
  private lifecycleSubscription?: Subscription;
  private connectionSubscription?: Subscription;
  pedidosEnProgreso = new Set<number | null | undefined>();
  pagosEnProgreso = new Set<number | null | undefined>();
  textoWhatsApp = '';
  readonly cremasDisponibles = ['Mayonesa', 'Ketchup', 'Aji', 'Mostaza', 'Tartara', 'Golf'];
  
  formPedido: Pedido = {
    requestId: '',
    customerName: '',
    customerPhone: '',
    customerAddress: '',
    deliveryCost: 5.0,
    type: 'DELIVERY',
    paymentMethod: 'EFECTIVO',
    paymentStatus: 'PENDIENTE',
    status: 'PREPARANDO',
    detalles: [],
    total: 5.0
  };

  constructor(
    private http: HttpClient,
    private toastService: ToastService,
    private appLifecycle: AppLifecycleService
  ) {}

  ngOnInit() {
    this.cargarTodosLosPedidos();
    this.cargarProductos();

    // Polling automatico cada 10 segundos
    this.intervalId = setInterval(() => {
      this.cargarTodosLosPedidos();
    }, 10000);

    // Escuchar cambios de visibilidad de pestana para ahorrar llamadas e impedir acumulacion de audio
    document.addEventListener('visibilitychange', this.onVisibilityChange);
    this.lifecycleSubscription = this.appLifecycle.refresh$.subscribe(() => {
      if (document.visibilityState === 'visible') {
        this.cargarTodosLosPedidos();
        this.cargarProductos();
      }
    });
    this.connectionSubscription = this.appLifecycle.online$.subscribe(online => {
      if (!online && this.intervalId) {
        clearInterval(this.intervalId);
        this.intervalId = null;
      } else if (online && document.visibilityState === 'visible' && !this.intervalId) {
        this.intervalId = setInterval(() => this.cargarTodosLosPedidos(), 10000);
      }
    });
  }

  ngOnDestroy() {
    document.removeEventListener('visibilitychange', this.onVisibilityChange);
    this.lifecycleSubscription?.unsubscribe();
    this.connectionSubscription?.unsubscribe();
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
    if (this.audioCtx) {
      this.audioCtx.close().catch(() => {});
    }
    if (this.telefonoBusquedaTimeout) {
      clearTimeout(this.telefonoBusquedaTimeout);
    }
  }

  onVisibilityChange = () => {
    if (document.visibilityState === 'visible') {
      if (navigator.onLine && !this.intervalId) {
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
    if (this.cargandoPedidosActivos) return;
    this.cargandoPedidosActivos = true;
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
        this.cargandoPedidosActivos = false;
        if (!this.entregadosPrecargados) {
          this.entregadosPrecargados = true;
          this.cargarPedidosEntregados(false);
        }
      },
      error: (err) => {
        this.cargandoPedidosActivos = false;
        console.error('Error al cargar pedidos del kanban', err);
      }
    });
  }

  actualizarEstado(pedidoId: number | null | undefined, nuevoEstado: string) {
    if (pedidoId === null || pedidoId === undefined) return;
    if (this.pedidosEnProgreso.has(pedidoId)) return;
    this.pedidosEnProgreso.add(pedidoId);

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
      this.entregadosCargadosAt = 0;
    }

    this.http.put(`${API_BASE_URL}/api/v1/pedidos/${pedidoId}/estado?nuevoEstado=${nuevoEstado}`, {}).subscribe({
      next: () => {
        this.pedidosEnProgreso.delete(pedidoId);
        // Cargar en segundo plano para verificar consistencia
        this.cargarTodosLosPedidos();
      },
      error: (err) => {
        this.pedidosEnProgreso.delete(pedidoId);
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
    this.pedidoEditandoId = null;
    this.mostrarDatosCliente = false;
    this.textoWhatsApp = '';
    this.formPedido = {
      requestId: this.nuevoRequestId(),
      clienteId: null,
      customerName: '',
      customerPhone: '',
      customerAddress: '',
      customerEmail: '',
      distrito: '',
      deliveryCost: 5.0,
      type: 'DELIVERY',
      paymentMethod: 'EFECTIVO',
      paymentStatus: 'PENDIENTE',
      status: 'PREPARANDO',
      detalles: [] as any[],
      total: 5.0
    };
    this.clienteEncontrado = false;
    this.buscandoCliente = false;
    this.mostrarModalNuevoPedido = true;
  }

  abrirEditarPedido(pedido: any) {
    this.pedidoEditandoId = pedido.id;
    this.mostrarDatosCliente = pedido.customerPhone !== '000000000'
      || (!!pedido.customerName && pedido.customerName !== 'Anonimo')
      || !!pedido.customerEmail;
    this.textoWhatsApp = '';
    this.clienteEncontrado = !!pedido.clienteId;
    this.formPedido = {
      requestId: pedido.requestId || this.nuevoRequestId(),
      clienteId: pedido.clienteId || null,
      customerName: pedido.customerName || '',
      customerPhone: pedido.customerPhone === '000000000' ? '' : (pedido.customerPhone || ''),
      customerAddress: pedido.type === 'PICKUP' ? '' : (pedido.customerAddress || ''),
      customerEmail: pedido.customerEmail || '',
      distrito: '',
      deliveryCost: Number(pedido.deliveryCost || 0),
      type: pedido.type,
      paymentMethod: ['EFECTIVO', 'YAPE'].includes(pedido.paymentMethod) ? pedido.paymentMethod : 'OTRO',
      paymentStatus: pedido.paymentStatus || 'PENDIENTE',
      status: pedido.status,
      detalles: (pedido.detalles || []).map((detalle: any) => ({
        productoId: detalle.productoId,
        productoName: detalle.productoName,
        productoPrice: Number(detalle.productoPrice || 0),
        quantity: detalle.quantity,
        subtotal: Number(detalle.subtotal || 0),
        creams: this.normalizarCremas(detalle.creams || ''),
        extraChaufa: !!detalle.extraChaufa,
        noCoincide: false
      })),
      total: Number(pedido.total || 0)
    };
    this.recalcularTotal();
    this.mostrarModalNuevoPedido = true;
  }

  cerrarModalNuevoPedido() {
    this.secuenciaBusquedaCliente++;
    if (this.telefonoBusquedaTimeout) {
      clearTimeout(this.telefonoBusquedaTimeout);
      this.telefonoBusquedaTimeout = null;
    }
    this.mostrarModalNuevoPedido = false;
    this.pedidoEditandoId = null;
    this.mostrarDatosCliente = false;
  }

  toggleDatosCliente() {
    this.mostrarDatosCliente = !this.mostrarDatosCliente;
    if (!this.mostrarDatosCliente) {
      this.formPedido.clienteId = null;
      this.formPedido.customerName = '';
      this.formPedido.customerPhone = '';
      this.formPedido.customerEmail = '';
      this.clienteEncontrado = false;
    }
  }

  buscarClientePorTelefono(telefono: string) {
    this.formPedido.clienteId = null;
    this.clienteEncontrado = false;
    this.buscandoCliente = false;
    this.secuenciaBusquedaCliente++;
    const secuenciaActual = this.secuenciaBusquedaCliente;

    if (this.telefonoBusquedaTimeout) {
      clearTimeout(this.telefonoBusquedaTimeout);
    }

    const telefonoLimpio = (telefono || '').replace(/\D/g, '');
    if (telefonoLimpio.length < 7) {
      return;
    }

    this.telefonoBusquedaTimeout = setTimeout(() => {
      this.buscandoCliente = true;
      this.http.get<any>(`${API_BASE_URL}/api/v1/clientes/buscar-por-telefono?telefono=${encodeURIComponent(telefonoLimpio)}`).subscribe({
        next: (cliente) => {
          if (secuenciaActual !== this.secuenciaBusquedaCliente || !this.mostrarModalNuevoPedido) return;
          if (!cliente) {
            this.buscandoCliente = false;
            return;
          }
          this.formPedido.clienteId = cliente.id;
          this.formPedido.customerName = cliente.name || '';
          this.formPedido.customerEmail = cliente.email || '';
          this.formPedido.customerAddress = cliente.address || '';
          this.clienteEncontrado = true;
          this.buscandoCliente = false;
        },
        error: (err) => {
          if (secuenciaActual !== this.secuenciaBusquedaCliente) return;
          this.buscandoCliente = false;
          console.error('Error al buscar cliente por telefono', err);
        }
      });
    }, 450);
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
    const parsedCreams = creamsMatch ? this.normalizarCremas(creamsMatch[1]) : '';
    const parsedPaymentStr = paymentMatch ? paymentMatch[1].trim().toUpperCase() : 'EFECTIVO';
    const parsedEmail = emailMatch ? emailMatch[1].trim() : '';

    const isPickup = parsedTypeStr.includes('RECOJO') || parsedTypeStr.includes('PICKUP') || parsedTypeStr.includes('LOCAL');
    const type = isPickup ? 'PICKUP' : 'DELIVERY';
    const deliveryCost = isPickup ? 0.00 : 5.00;

    let paymentMethod = 'EFECTIVO';
    if (parsedPaymentStr.includes('YAPE')) paymentMethod = 'YAPE';
    else if (!parsedPaymentStr.includes('EFECTIVO')) paymentMethod = 'OTRO';

    // Procesar lineas de productos
    const detalles: any[] = [];
    const lines = text.split('\n');
    const cantidadLineasProducto = lines.filter(linea =>
      /^[-\*\d\.\s]*\d+x\s*/i.test(linea.trim())
    ).length;
    let inDetail = false;

    for (let lineIndex = 0; lineIndex < lines.length; lineIndex++) {
      const cleanLine = lines[lineIndex].trim();
      if (cleanLine.toLowerCase().includes('detalle del pedido')) {
        inDetail = true;
        continue;
      }
      if (cleanLine.toLowerCase().includes('total de productos') || cleanLine.toLowerCase().includes('total:')) {
        inDetail = false;
      }

      // Detectar lineas de productos con mayor flexibilidad
      const isProductLine = cleanLine.match(/^[-\*\d\.\s]*\d+x\s*/i);
      if (inDetail && isProductLine) {
        const match = cleanLine.match(/^[-\*\d\.\s]*(\d+)x\s*([^(]+)/i);
        if (match) {
          const quantity = parseInt(match[1], 10);
          let productName = match[2].trim();

          productName = this.limpiarNombreProducto(productName);

          // Une la linea del producto con su configuracion inmediata. Algunos mensajes
          // colocan cremas y chaufa en las siguientes lineas.
          let configuracionProducto = cleanLine;
          for (let offset = 1; offset <= 4 && lineIndex + offset < lines.length; offset++) {
            const lineaSiguiente = lines[lineIndex + offset].trim();
            if (!lineaSiguiente) continue;
            if (/^[-\*\d\.\s]*\d+x\s*/i.test(lineaSiguiente)
              || /total de productos|total\s*:/i.test(lineaSiguiente)) {
              break;
            }
            configuracionProducto += ` | ${lineaSiguiente}`;
          }

          let creamsDeFila = cantidadLineasProducto === 1 ? parsedCreams : '';
          const creamsInLineMatch = configuracionProducto.match(/(?:cremas?|salsas?)[^:]*:\s*\**([^\)\|\n]+)/i);
          if (creamsInLineMatch) {
            creamsDeFila = this.normalizarCremas(creamsInLineMatch[1]);
          }
          if (/sin\s+(?:cremas?|salsas?)/i.test(configuracionProducto)) {
            creamsDeFila = '';
          }

          // Buscar coincidencia ultra-robusta en el catalogo
          const product = this.buscarCoincidencia(productName);

          if (product) {
            const extraChaufa = /\bcon\s+chaufa\b/i.test(configuracionProducto)
              && this.productoPermiteChaufa(product.id);
            detalles.push({
              productoId: product.id,
              productoName: product.name,
              productoPrice: product.price,
              quantity: quantity,
              subtotal: quantity * (product.price + (extraChaufa ? 4 : 0)),
              creams: creamsDeFila,
              extraChaufa: extraChaufa
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
              extraChaufa: false,
              noCoincide: true
            });
          }
        }
      }
    }

    this.formPedido = {
      requestId: this.nuevoRequestId(),
      clienteId: null,
      customerName: parsedName,
      customerPhone: parsedPhone,
      customerAddress: parsedAddress,
      customerEmail: parsedEmail,
      distrito: parsedDistrict,
      deliveryCost: deliveryCost,
      type: type,
      paymentMethod: paymentMethod,
      paymentStatus: 'PENDIENTE',
      status: 'PREPARANDO',
      detalles: detalles,
      total: 0.0
    };

    this.mostrarDatosCliente = !!(parsedName || parsedPhone || parsedEmail);
    this.recalcularTotal();
    if (parsedPhone) {
      this.buscarClientePorTelefono(parsedPhone);
    }
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

  private normalizarCremas(valor: string): string {
    if (!valor || /sin\s+(?:cremas?|salsas?)/i.test(valor)) return '';

    const cremas = valor
      .replace(/\*/g, '')
      .replace(/\s+y\s+/gi, ',')
      .split(/[,;\/|]+/)
      .map(crema => {
        const nombre = this.normalizarParaComparar(crema);
        if (nombre === 'mayo' || nombre.includes('mayonesa')) return 'Mayonesa';
        if (nombre.includes('ketchup') || nombre.includes('catsup') || nombre.includes('catchup')) return 'Ketchup';
        if (nombre.includes('aji')) return 'Aji';
        if (nombre.includes('mostaza')) return 'Mostaza';
        if (nombre.includes('tartara')) return 'Tartara';
        if (nombre.includes('golf')) return 'Golf';
        return '';
      })
      .filter(Boolean);

    return [...new Set(cremas)].join(', ');
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
      extraChaufa: false,
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
    detalle.subtotal = detalle.quantity * this.precioUnitarioDetalle(detalle);
    this.recalcularTotal();
  }

  onProductoChange(detalle: any, event: any) {
    const val = event.target.value;
    if (!val) {
      detalle.productoId = null;
      detalle.productoName = '';
      detalle.productoPrice = 0.0;
      detalle.subtotal = 0.0;
      detalle.extraChaufa = false;
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
      if (!this.productoPermiteChaufa(product.id)) {
        detalle.extraChaufa = false;
      }
      detalle.subtotal = detalle.quantity * this.precioUnitarioDetalle(detalle);
      detalle.noCoincide = false;
      this.recalcularTotal();
    }
  }

  recalcularTotal() {
    this.formPedido.deliveryCost = this.formPedido.type === 'PICKUP' ? 0.00 : 5.00;
    const subtotal = this.formPedido.detalles.reduce((sum: number, d: any) => sum + (d.subtotal || 0), 0);
    this.formPedido.total = subtotal + this.formPedido.deliveryCost;
  }

  productoPermiteChaufa(productoId: number | null): boolean {
    return productoId !== null && [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 14, 16, 20, 28, 29, 30].includes(productoId);
  }

  productoPermiteCremas(productoId: number | null): boolean {
    return productoId !== null && ![21, 22, 23, 24].includes(productoId);
  }

  toggleChaufa(detalle: any) {
    if (!this.productoPermiteChaufa(detalle.productoId)) return;
    detalle.extraChaufa = !detalle.extraChaufa;
    detalle.subtotal = detalle.quantity * this.precioUnitarioDetalle(detalle);
    this.recalcularTotal();
  }

  toggleCrema(detalle: any, crema: string) {
    const seleccionadas = this.normalizarCremas(detalle.creams || '')
      .split(',')
      .map((valor: string) => valor.trim())
      .filter(Boolean);
    const indice = seleccionadas.indexOf(crema);
    if (indice >= 0) {
      seleccionadas.splice(indice, 1);
    } else {
      seleccionadas.push(crema);
    }
    detalle.creams = seleccionadas.join(', ');
  }

  cremaSeleccionada(detalle: any, crema: string): boolean {
    return this.normalizarCremas(detalle.creams || '')
      .split(',')
      .map((valor: string) => valor.trim())
      .includes(crema);
  }

  private precioUnitarioDetalle(detalle: any): number {
    return Number(detalle.productoPrice || 0) + (detalle.extraChaufa ? 4 : 0);
  }

  guardarNuevoPedido() {
    if (this.guardandoPedido) return;

    if (this.formPedido.detalles.length === 0) {
      this.toastService.error('Debe agregar al menos un producto al pedido.');
      return;
    }

    const tieneNoCoincidentes = this.formPedido.detalles.some((d: any) => !d.productoId);
    if (tieneNoCoincidentes) {
      this.toastService.warning('Por favor, selecciona un producto valido para todos los elementos.');
      return;
    }

    this.guardandoPedido = true;

    // Limpiar el payload enviando unicamente los campos esperados por el backend DTO
    const cleanDetalles = this.formPedido.detalles.map((d: any) => ({
      productoId: d.productoId,
      quantity: d.quantity,
      creams: d.creams,
      extraChaufa: !!d.extraChaufa
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
      requestId: this.formPedido.requestId || (this.formPedido.requestId = this.nuevoRequestId()),
      clienteId: this.formPedido.clienteId,
      customerName: this.formPedido.customerName,
      customerPhone: this.formPedido.customerPhone,
      customerAddress: finalAddress,
      customerEmail: this.formPedido.customerEmail,
      deliveryCost: this.formPedido.deliveryCost,
      type: this.formPedido.type,
      paymentMethod: this.formPedido.paymentMethod,
      paymentStatus: this.formPedido.paymentStatus || 'PENDIENTE',
      detalles: cleanDetalles
    };

    const pedidoId = this.pedidoEditandoId;
    const solicitud = pedidoId
      ? this.http.put(`${API_BASE_URL}/api/v1/pedidos/${pedidoId}`, payload)
      : this.http.post(`${API_BASE_URL}/api/v1/pedidos`, payload);

    solicitud.subscribe({
      next: () => {
        this.guardandoPedido = false;
        this.cargarTodosLosPedidos();
        this.cerrarModalNuevoPedido();
        this.toastService.success(pedidoId ? 'Pedido actualizado con exito.' : 'Pedido registrado con exito.');
      },
      error: (err) => {
        this.guardandoPedido = false;
        console.error('Error al guardar pedido', err);
        const mensaje = typeof err?.error?.error === 'string'
          ? err.error.error
          : 'Ocurrio un error al registrar el pedido.';
        this.toastService.error(mensaje);
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
  private entregadosPrecargados = false;
  private entregadosCargadosAt = 0;

  verPedidosEntregadosHoy() {
    this.mostrarModalEntregados = true;
    if (Date.now() - this.entregadosCargadosAt < 30000) return;
    this.cargarPedidosEntregados(true);
  }

  private cargarPedidosEntregados(mostrarError: boolean) {
    if (this.cargandoEntregados) return;
    this.cargandoEntregados = true;
    this.http.get<any[]>(`${API_BASE_URL}/api/v1/pedidos/entregados-hoy`).subscribe({
      next: (data) => {
        this.pedidosEntregadosHoy = data;
        this.entregadosCargadosAt = Date.now();
        this.cargandoEntregados = false;
      },
      error: (err) => {
        console.error('Error al cargar entregados de hoy', err);
        this.cargandoEntregados = false;
        if (mostrarError) this.toastService.error('No se pudieron cargar los pedidos entregados.');
      }
    });
  }

  actualizarPago(pedido: Pedido, nuevoEstado: 'PENDIENTE' | 'PAGADO') {
    if (pedido.id === null || pedido.id === undefined) return;
    if (this.pagosEnProgreso.has(pedido.id)) return;
    this.pagosEnProgreso.add(pedido.id);
    this.http.put<any>(`${API_BASE_URL}/api/v1/pedidos/${pedido.id}/pago?nuevoEstado=${nuevoEstado}`, {}).subscribe({
      next: (actualizado) => {
        pedido.paymentStatus = actualizado.paymentStatus;
        this.pagosEnProgreso.delete(pedido.id);
        this.toastService.success(nuevoEstado === 'PAGADO' ? 'Pago confirmado.' : 'Pago marcado como pendiente.');
      },
      error: (err) => {
        this.pagosEnProgreso.delete(pedido.id);
        console.error('Error al actualizar el pago', err);
        this.toastService.error('No se pudo actualizar el estado de pago.');
      }
    });
  }

  private nuevoRequestId(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
      return crypto.randomUUID();
    }
    return `${Date.now()}-${Math.random().toString(36).slice(2)}`;
  }

  normalizarFechaUtc(fecha: string | undefined): string {
    if (!fecha) return '';
    return /Z$|[+-]\d{2}:?\d{2}$/.test(fecha) ? fecha : `${fecha}Z`;
  }

  private fechaCalendarioLima(fecha: Date): string {
    return new Intl.DateTimeFormat('en-CA', {
      timeZone: 'America/Lima',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    }).format(fecha);
  }

  cerrarModalEntregados() {
    this.mostrarModalEntregados = false;
  }

  setDeliveryType(type: string) {
    this.formPedido.type = type;
    if (type === 'PICKUP') {
      this.formPedido.distrito = 'Surquillo';
      this.formPedido.customerAddress = 'Retiro en local';
    } else {
      if (this.formPedido.customerAddress === 'Retiro en local') {
        this.formPedido.customerAddress = '';
      }
      if (this.formPedido.distrito === 'Surquillo') {
        this.formPedido.distrito = '';
      }
    }
    this.recalcularTotal();
  }
}


