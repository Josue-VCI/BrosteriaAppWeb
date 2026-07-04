export interface Cliente {
  id?: number | null;
  name: string;
  email?: string;
  phone: string;
  address?: string;
  totalOrders?: number;
  totalSpent?: number;
  points?: number;
  createdAt?: string;
  calle?: string;
  distrito?: string;
}

export interface Insumo {
  id?: number | null;
  name: string;
  quantity: number;
  unit: string;
  minimumStock: number;
  lastAlertedAt?: string;
  updatedAt?: string;
}

export interface Producto {
  id: number;
  name: string;
  description?: string;
  price: number;
  category: string;
  imageUrl?: string;
  active: boolean;
}

export interface DetallePedido {
  id?: number | null;
  productoId: number | null;
  productoName?: string;
  productoPrice?: number;
  quantity: number;
  subtotal?: number;
  creams?: any;
  noCoincide?: boolean;
}

export interface Pedido {
  id?: number | null;
  requestId?: string;
  customerName: string;
  customerPhone: string;
  customerAddress: string;
  deliveryCost: number;
  type: string; // 'DELIVERY', 'PICKUP'
  paymentMethod: string; // 'YAPE', 'PLIN', 'EFECTIVO', 'TARJETA'
  paymentStatus: string; // 'PENDIENTE', 'PAGADO'
  status: string; // 'PREPARANDO', 'ENVIADO', 'ENTREGADO', 'CANCELADO'
  detalles: DetallePedido[];
  total?: number;
  orderDate?: string;
  paidAt?: string;
  clienteId?: number | null;
  customerEmail?: string;
  distrito?: string;
}

export interface LoginResponse {
  token: string;
  id: number;
  name: string;
  role: string;
}

export interface DashboardResumen {
  ventasTotales: number;
  totalPedidos: number;
  completados: number;
  cancelados: number;
}
