import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

export const authGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const token = localStorage.getItem('brosteria_token');
  if (!token) {
    return router.createUrlTree(['/login']);
  }
  
  const role = localStorage.getItem('brosteria_role');
  const path = route.routeConfig?.path;
  
  if ((path === 'dashboard' || path === 'reportes') && role !== 'ADMIN') {
    return router.createUrlTree(['/pedidos']);
  }
  
  return true;
};
