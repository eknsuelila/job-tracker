import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Route guards decide whether navigation to a route is allowed.
 * Returning a UrlTree (router.createUrlTree) tells the router to redirect instead.
 *
 * NOTE: guards only improve UX. Real security is enforced by the backend, which rejects
 * any API call without a valid token no matter what the browser does.
 */

/** Only logged-in users may enter; others are sent to /login. */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isLoggedIn() ? true : inject(Router).createUrlTree(['/login']);
};

/** Login/register pages: logged-in users are sent straight to the dashboard. */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isLoggedIn() ? inject(Router).createUrlTree(['/dashboard']) : true;
};
