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
  esAdmin = false;

  constructor(private router: Router) {}

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
  }

  private actualizarSidebarYRol() {
    this.mostrarSidebar = this.router.url !== '/login';
    this.usuarioNombre = localStorage.getItem('brosteria_username') || 'Josue Espinoza (Admin)';
    this.esAdmin = localStorage.getItem('brosteria_role') === 'ADMIN';
  }

  logout() {
    localStorage.removeItem('brosteria_token');
    localStorage.removeItem('brosteria_username');
    localStorage.removeItem('brosteria_role');
    this.router.navigate(['/login']);
  }
}
