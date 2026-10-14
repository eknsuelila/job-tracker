import { ApplicationConfig } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { authInterceptor } from './core/auth/auth.interceptor';

/**
 * Application-wide providers (this replaces the old AppModule in standalone Angular).
 *
 * - provideRouter: enables routing. withComponentInputBinding() lets route params such as
 *   ":id" be received directly as component @Input()s.
 * - provideHttpClient: enables HttpClient and plugs in our JWT interceptor.
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor])),
  ],
};
