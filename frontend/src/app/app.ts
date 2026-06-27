import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, NavigationEnd, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { filter } from 'rxjs/operators';
import { ToastService, ToastMessage } from './services/toast.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class AppComponent implements OnInit, OnDestroy {
  mostrarSidebar = false;
  sidebarAbierto = false;
  usuarioNombre = 'Administrador (Demo)';
  esAdmin = false;

  toasts: ToastMessage[] = [];
  private toastSubscription!: Subscription;
  private timeoutId: any;
  private eventListeners: { name: string; handler: any }[] = [];

  constructor(private router: Router, private toastService: ToastService) {}

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

    // Inicializar tracker de inactividad
    this.iniciarTrackerInactividad();
  }

  ngOnDestroy() {
    if (this.toastSubscription) {
      this.toastSubscription.unsubscribe();
    }
    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
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

  // Tracker de Inactividad (5 Minutos)
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
      }, 5 * 60 * 1000); // 5 minutos
    }
  }

  private logoutPorInactividad() {
    this.logout();
    this.toastService.warning('Tu sesion ha expirado por inactividad de 5 minutos.', 8000);
  }
}
