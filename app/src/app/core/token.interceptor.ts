import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth';

export const tokenInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.token();
  const url = req.url;

  const isApi = url.startsWith('/api');
  const authReq = isApi && token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(authReq).pipe(
    catchError((error) => {
      if (error?.status === 401) {
        auth.clearSession();
        const current = router.url;
        if (!current.startsWith('/auth')) {
          router.navigate(['/auth/login'], { queryParams: { returnUrl: current } });
        }
      }
      return throwError(() => error);
    }),
  );
};
