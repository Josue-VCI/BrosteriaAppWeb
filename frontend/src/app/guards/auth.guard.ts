import { CanActivateFn } from '@angular/router';

export const authGuard: CanActivateFn = (route, state) => {
  // Seguridad desactivada temporalmente: siempre permitir acceso
  return true;
};
