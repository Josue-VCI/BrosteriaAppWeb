import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { HttpInterceptorFn } from '@angular/common/http';

import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { catchError, throwError, timeout, retry } from 'rxjs';
import { ToastService } from './services/toast.service';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('brosteria_token');
  const router = inject(Router);
  const toastService = inject(ToastService);
  
  let authReq = req;
  if (token) {
    authReq = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }

  let requestPipeline = next(authReq).pipe(
    timeout(12000), // Timeout de 12 segundos para evitar que la interfaz se quede colgada indefinidamente
    catchError((error) => {
      // Manejar error de timeout de RxJS
      if (error.name === 'TimeoutError') {
        toastService.error('El servidor esta tardando demasiado en responder. Reintentando...');
        return throwError(() => new Error('TimeoutError'));
      }

      const status = error.status;
      if (status === 401 || status === 403) {
        localStorage.removeItem('brosteria_token');
        localStorage.removeItem('brosteria_username');
        localStorage.removeItem('brosteria_role');
        router.navigate(['/login']);
      } else if (status === 0) {
        toastService.warning('Error de conexion con el servidor. Reintentando...');
      } else if (status >= 500) {
        toastService.error('Ocurrio un error en el servidor. Por favor, reintente en unos momentos.');
      }
      return throwError(() => error);
    })
  );

  // Auto-reintento con delay de 1.5s solo para solicitudes de lectura (GET) para tolerar microcaidas de conexion
  if (req.method === 'GET') {
    requestPipeline = requestPipeline.pipe(
      retry({ count: 2, delay: 1500 })
    );
  }

  return requestPipeline;
};

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([jwtInterceptor])),
    provideAnimations()
  ]
};
