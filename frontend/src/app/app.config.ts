import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { HttpInterceptorFn } from '@angular/common/http';

import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { catchError, throwError, timeout, retry, timer } from 'rxjs';
import { ToastService } from './services/toast.service';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('brosteria_token');
  const router = inject(Router);
  const toastService = inject(ToastService);
  
  let authReq = req;
  const esPeticionLogin = req.url.includes('/api/v1/auth/login');
  if (token && !esPeticionLogin) {
    authReq = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }

  const timeoutMs = req.method === 'GET' ? 15000 : 45000;
  const requestPipeline = next(authReq).pipe(
    timeout(timeoutMs),
    retry({
      count: req.method === 'GET' ? 2 : 0,
      delay: (error) => {
        const status = error instanceof HttpErrorResponse ? error.status : undefined;
        const esTransitorio = error?.name === 'TimeoutError' || status === 0 || (status !== undefined && status >= 500);
        return esTransitorio ? timer(1500) : throwError(() => error);
      }
    }),
    catchError((error) => {
      if (error.name === 'TimeoutError') {
        toastService.error('El servidor esta tardando demasiado en responder.');
        return throwError(() => error);
      }

      const status = error.status;
      if (status === 401) {
        localStorage.removeItem('brosteria_token');
        localStorage.removeItem('brosteria_username');
        localStorage.removeItem('brosteria_role');
        toastService.warning('Tu sesion expiro. Inicia sesion nuevamente.');
        router.navigate(['/login']);
      } else if (status === 403) {
        toastService.warning('Tu usuario no tiene permiso para realizar esta accion.');
      } else if (status === 0) {
        toastService.warning('Error de conexion con el servidor.');
      } else if (status >= 500) {
        toastService.error('Ocurrio un error en el servidor. Por favor, reintente en unos momentos.');
      }
      return throwError(() => error);
    })
  );

  return requestPipeline;
};

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([jwtInterceptor])),
    provideAnimations()
  ]
};
