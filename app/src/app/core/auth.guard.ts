import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from './auth';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isAuthenticated()) return true;

  const returnUrl = state.url;
  const isInternal = returnUrl.startsWith('/') && !returnUrl.startsWith('//');
  return router.createUrlTree(['/auth/login'], {
    queryParams: isInternal ? { returnUrl } : {},
  });
};
