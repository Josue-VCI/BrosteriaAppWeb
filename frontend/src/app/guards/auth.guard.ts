import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

export const authGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  if (localStorage.getItem('brosteria_token')) {
    return true;
  }
  return router.createUrlTree(['/login']);
};
