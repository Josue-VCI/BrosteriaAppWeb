import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, NavigationEnd, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { filter } from 'rxjs/operators';
import { ToastService, ToastMessage } from './services/toast.service';
import { Subscription } from 'rxjs';
import { AppLifecycleService } from './services/app-lifecycle.service';

@Component({
    selector: 'app-root',
    imports: [CommonModule, RouterModule],
    templateUrl: './app.html',
    styleUrls: ['./app.css']
})
export class AppComponent implements OnInit, OnDestroy {
  private static readonly INACTIVIDAD_MAXIMA_MS = 12 * 60 * 60 * 1000;
  mostrarSidebar = false;
  sidebarAbierto = false;
  usuarioNombre = 'Administrador (Demo)';
  esAdmin = false;

  toasts: ToastMessage[] = [];
  private toastSubscription!: Subscription;
  private connectionSubscription!: Subscription;
  private timeoutId: any;
  private reconnectTimeoutId: any;
  private eventListeners: { name: string; handler: any }[] = [];
  sinConexion = !navigator.onLine;
  reconectando = false;
  private estuvoSinConexion = !navigator.onLine;

  constructor(
    private router: Router,
    private toastService: ToastService,
    private appLifecycle: AppLifecycleService
  ) {}

  toggleSidebar() {
    this.sidebarAbierto = !this.sidebarAbierto;
  }

  ngOnInit() {
    this.actualizarSidebarYRol();

    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      this.actualizarSidebarYRol();
      this.sidebarAbierto = false;
    });

    // Suscripcion a Toasts
    this.toastSubscription = this.toastService.toasts$.subscribe(toast => {
      this.toasts.push(toast);
      setTimeout(() => {
        this.removeToast(toast.id);
      }, toast.duration || 4000);
    });

    this.connectionSubscription = this.appLifecycle.online$.subscribe(online => {
      this.sinConexion = !online;
      if (!online) {
        this.estuvoSinConexion = true;
        this.reconectando = false;
        return;
      }
      if (this.estuvoSinConexion) {
        this.estuvoSinConexion = false;
        this.reconectando = true;
        clearTimeout(this.reconnectTimeoutId);
        this.reconnectTimeoutId = setTimeout(() => this.reconectando = false, 4000);
      }
    });

    // Inicializar tracker de inactividad
    this.iniciarTrackerInactividad();
  }

  ngOnDestroy() {
    if (this.toastSubscription) {
      this.toastSubscription.unsubscribe();
    }
    if (this.connectionSubscription) {
      this.connectionSubscription.unsubscribe();
    }
    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
    }
    if (this.reconnectTimeoutId) {
      clearTimeout(this.reconnectTimeoutId);
    }
    // Remover event listeners
    this.eventListeners.forEach(el => {
      document.removeEventListener(el.name, el.handler);
    });
  }

  removeToast(id: number) {
    this.toasts = this.toasts.filter(t => t.id !== id);
  }

  private actualizarSidebarYRol() {
    this.mostrarSidebar = this.router.url !== '/login';
    this.usuarioNombre = localStorage.getItem('brosteria_username') || 'Josue Espinoza (Admin)';
    this.esAdmin = localStorage.getItem('brosteria_role') === 'ADMIN';
    this.reiniciarTimerInactividad();
  }

  logout() {
    localStorage.removeItem('brosteria_token');
    localStorage.removeItem('brosteria_username');
    localStorage.removeItem('brosteria_role');
    this.router.navigate(['/login']);
  }

  // Tracker de inactividad para una jornada completa.
  private iniciarTrackerInactividad() {
    const handler = () => this.reiniciarTimerInactividad();
    const eventos = ['mousemove', 'click', 'keypress', 'scroll', 'touchstart'];
    
    eventos.forEach(evt => {
      document.addEventListener(evt, handler);
      this.eventListeners.push({ name: evt, handler });
    });

    this.reiniciarTimerInactividad();
  }

  private reiniciarTimerInactividad() {
    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
    }
    // Solo si esta logueado
    if (localStorage.getItem('brosteria_token')) {
      this.timeoutId = setTimeout(() => {
        this.logoutPorInactividad();
      }, AppComponent.INACTIVIDAD_MAXIMA_MS);
    }
  }

  private logoutPorInactividad() {
    this.logout();
    this.toastService.warning('Tu sesion ha expirado despues de 12 horas sin actividad.', 8000);
  }
}
