import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { errorMessage } from '../../shared/error-message';

/**
 * Login page built with a REACTIVE FORM.
 *
 * Reactive forms are defined in TypeScript (not in the HTML), which makes them easy to
 * validate, test and reason about. The template binds to them with [formGroup] and formControlName.
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './auth-page.scss',
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** nonNullable: reset() goes back to '' instead of null, which keeps types simple. */
  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  /** Pre-fills the seeded demo account, handy for recruiters trying the app. */
  useDemo(): void {
    this.form.setValue({ email: 'demo@example.com', password: 'password123' });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched(); // reveal validation messages on every field
      return;
    }
    this.loading.set(true);
    this.error.set(null);

    const { email, password } = this.form.getRawValue();
    this.auth.login(email, password).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (err) => {
        this.error.set(errorMessage(err, 'Login failed'));
        this.loading.set(false);
      },
    });
  }
}
