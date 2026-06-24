import { Component, OnInit } from '@angular/core';
import { Router, NavigationEnd, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class AppComponent implements OnInit {
  mostrarSidebar = false;
  sidebarAbierto = false;
  usuarioNombre = 'Administrador (Demo)';

  constructor(private router: Router) {
    // Inicializar sesión por defecto para evitar pantallas de login por la desactivación de seguridad
    if (!localStorage.getItem('brosteria_token')) {
      localStorage.setItem('brosteria_token', 'token_demo_seguridad_desactivada');
      localStorage.setItem('brosteria_username', 'Josue Espinoza (Admin)');
    }
  }

  toggleSidebar() {
    this.sidebarAbierto = !this.sidebarAbierto;
  }

  ngOnInit() {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      // Mostrar la barra de navegación lateral en todas las vistas excepto login
      this.mostrarSidebar = this.router.url !== '/login';
      this.usuarioNombre = localStorage.getItem('brosteria_username') || 'Josue Espinoza (Admin)';
      this.sidebarAbierto = false;
    });
  }

  logout() {
    localStorage.removeItem('brosteria_token');
    localStorage.removeItem('brosteria_username');
    this.router.navigate(['/login']);
  }
}
