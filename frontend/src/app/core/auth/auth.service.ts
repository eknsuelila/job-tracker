import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { AuthResponse, User } from '../models';

/** localStorage keys. The token survives page refreshes until the user logs out. */
const TOKEN_KEY = 'jobtracker.token';
const USER_KEY = 'jobtracker.user';

/**
 * Owns everything about "who is logged in".
 *
 * `providedIn: 'root'` makes this a singleton shared by the whole app.
 *
 * State is held in Angular *signals*: reactive values that automatically update any template
 * or `computed()` that reads them. When `login()` sets `currentUser`, the navbar re-renders
 * on its own without manual change-detection code.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  // inject() is the modern alternative to constructor injection.
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  /** Private writable signal; components only get the read-only version below. */
  private readonly _currentUser = signal<User | null>(this.readStoredUser());

  readonly currentUser = this._currentUser.asReadonly();
  readonly isLoggedIn = computed(() => this._currentUser() !== null);

  get token(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/login', { email, password })
      // tap() runs a side effect (saving the session) without changing the emitted value.
      .pipe(tap((res) => this.storeSession(res)));
  }

  register(name: string, email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/register', { name, email, password })
      .pipe(tap((res) => this.storeSession(res)));
  }

  /** Clears the session and sends the user to the login page. JWTs are stateless, so there's no server call. */
  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this._currentUser.set(null);
    this.router.navigate(['/login']);
  }

  private storeSession(res: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, res.token);
    localStorage.setItem(USER_KEY, JSON.stringify(res.user));
    this._currentUser.set(res.user);
  }

  private readStoredUser(): User | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw || !localStorage.getItem(TOKEN_KEY)) {
      return null;
    }
    try {
      return JSON.parse(raw) as User;
    } catch {
      return null;
    }
  }
}
