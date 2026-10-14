import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth/auth.guard';

/**
 * URL -> component mapping.
 *
 * `loadComponent` lazy-loads each page: its code is split into a separate JavaScript file and
 * downloaded only when the user first visits that URL, which keeps the initial download small.
 */
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },

  // Public pages (guestGuard bounces already-logged-in users to the dashboard)
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/register.component').then((m) => m.RegisterComponent),
  },

  // Private pages (authGuard sends anonymous users to /login)
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
  },
  {
    path: 'applications',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/applications/application-list.component').then((m) => m.ApplicationListComponent),
  },
  {
    path: 'applications/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/applications/application-form.component').then((m) => m.ApplicationFormComponent),
  },
  {
    // ":id" becomes the `id` @Input of ApplicationFormComponent (see withComponentInputBinding)
    path: 'applications/:id/edit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/applications/application-form.component').then((m) => m.ApplicationFormComponent),
  },

  // Unknown URLs go home
  { path: '**', redirectTo: 'dashboard' },
];
