import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/**
 * HTTP interceptor: a function that runs for EVERY HttpClient request.
 *
 * 1. Adds the "Authorization: Bearer <jwt>" header so the backend knows who is calling.
 *    Requests are immutable, so we `clone()` the request with the extra header.
 * 2. If the server answers 401 (token expired or invalid), logs the user out so they
 *    land on the login page instead of seeing a broken screen.
 *
 * It is registered in app.config.ts with `provideHttpClient(withInterceptors([authInterceptor]))`.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token;

  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // Ignore 401s from the login endpoint itself: that just means "wrong password".
      if (error.status === 401 && !req.url.includes('/api/auth/')) {
        auth.logout();
      }
      return throwError(() => error);
    }),
  );
};
