import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  email = '';
  password = '';
  errorMsg = '';
  cargando = false;

  constructor(private http: HttpClient, private router: Router) {}

  onSubmit() {
    this.errorMsg = '';
    this.cargando = true;
    
    const payload = { email: this.email, password: this.password };
    
    this.http.post<any>('http://localhost:8081/api/v1/auth/login', payload).subscribe({
      next: (res) => {
        localStorage.setItem('brosteria_token', res.token);
        localStorage.setItem('brosteria_username', res.userName);
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.cargando = false;
        if (err.status === 403 || err.status === 401 || err.status === 400) {
          this.errorMsg = 'Correo o contraseña incorrectos';
        } else {
          this.errorMsg = 'No se pudo conectar al servidor. Intente nuevamente.';
        }
      }
    });
  }
}
