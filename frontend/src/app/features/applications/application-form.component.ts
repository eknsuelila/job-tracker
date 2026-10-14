import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { JobApplicationService } from '../../core/api/job-application.service';
import { APPLICATION_STATUSES, ApplicationStatus, JobApplicationRequest } from '../../core/models';
import { StatusLabelPipe } from '../../shared/status-label.pipe';
import { errorMessage } from '../../shared/error-message';

/** Today's date as "YYYY-MM-DD" in local time (the format <input type="date"> uses). */
function today(): string {
  const d = new Date();
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
}

/**
 * One component serves both "create" (/applications/new) and "edit" (/applications/:id/edit).
 * If the router provides an `id` input, we are editing; otherwise we are creating.
 */
@Component({
  selector: 'app-application-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, StatusLabelPipe],
  templateUrl: './application-form.component.html',
})
export class ApplicationFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(JobApplicationService);
  private readonly router = inject(Router);

  /** Filled automatically from the ":id" route parameter (withComponentInputBinding in app.config.ts). */
  @Input() id?: string;

  readonly statuses = APPLICATION_STATUSES;
  readonly maxDate = today();

  // Validators mirror the backend's JobApplicationRequest constraints.
  readonly form = this.fb.group({
    company: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(150)]),
    position: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(150)]),
    location: this.fb.nonNullable.control('', Validators.maxLength(150)),
    jobUrl: this.fb.nonNullable.control('', [Validators.maxLength(500), Validators.pattern(/^(https?:\/\/.*)?$/)]),
    salary: this.fb.control<number | null>(null, Validators.min(0)),
    status: this.fb.nonNullable.control<ApplicationStatus>('APPLIED'),
    appliedDate: this.fb.nonNullable.control(today(), Validators.required),
    notes: this.fb.nonNullable.control('', Validators.maxLength(2000)),
  });

  readonly saving = signal(false);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  get isEdit(): boolean {
    return !!this.id;
  }

  ngOnInit(): void {
    if (this.id) {
      this.loading.set(true);
      this.api.get(Number(this.id)).subscribe({
        next: (app) => {
          // patchValue copies matching keys from the API object into the form controls.
          this.form.patchValue({
            ...app,
            location: app.location ?? '',
            jobUrl: app.jobUrl ?? '',
            notes: app.notes ?? '',
          });
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(errorMessage(err, 'Could not load this application'));
          this.loading.set(false);
        },
      });
    }
  }

  /** True when a control should display its error message. */
  showError(name: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[name];
    return control.invalid && control.touched;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set(null);

    const body = this.form.getRawValue() as JobApplicationRequest;
    const request$ = this.id ? this.api.update(Number(this.id), body) : this.api.create(body);

    request$.subscribe({
      next: () => this.router.navigate(['/applications']),
      error: (err) => {
        this.error.set(errorMessage(err, 'Could not save'));
        this.saving.set(false);
      },
    });
  }
}
