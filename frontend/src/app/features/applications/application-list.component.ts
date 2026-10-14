import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { JobApplicationService } from '../../core/api/job-application.service';
import { APPLICATION_STATUSES, ApplicationStatus, JobApplication, Page } from '../../core/models';
import { StatusLabelPipe } from '../../shared/status-label.pipe';
import { errorMessage } from '../../shared/error-message';

type SortField = 'company' | 'position' | 'appliedDate' | 'status';

/**
 * The main table of applications with server-side search, filtering, sorting and pagination.
 *
 * "Server-side" means the browser only ever downloads one page of results; the database does
 * the filtering/sorting. This scales to thousands of rows, unlike filtering in the browser.
 */
@Component({
  selector: 'app-application-list',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, DatePipe, CurrencyPipe, StatusLabelPipe],
  templateUrl: './application-list.component.html',
  styleUrl: './application-list.component.scss',
})
export class ApplicationListComponent implements OnInit {
  private readonly api = inject(JobApplicationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly statuses = APPLICATION_STATUSES;

  // Filter inputs are FormControls so we can react to their valueChanges streams.
  readonly search = new FormControl('', { nonNullable: true });
  readonly statusFilter = new FormControl<ApplicationStatus | ''>('', { nonNullable: true });

  // Current query state
  readonly pageIndex = signal(0);
  readonly pageSize = 10;
  readonly sortField = signal<SortField>('appliedDate');
  readonly sortDir = signal<'asc' | 'desc'>('desc');

  // Results / UI state
  readonly page = signal<Page<JobApplication> | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  /** Id of the row whose "Delete?" confirmation is showing (inline confirm instead of a browser popup). */
  readonly confirmingDeleteId = signal<number | null>(null);

  ngOnInit(): void {
    // Wait 300 ms after the user stops typing before searching, and skip if the text didn't change.
    // takeUntilDestroyed unsubscribes automatically when the component is destroyed (no memory leaks).
    this.search.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.reload());

    this.statusFilter.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.reload());

    this.load();
  }

  /** Go back to page 1 (used whenever filters change) and fetch. */
  reload(): void {
    this.pageIndex.set(0);
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api
      .list({
        q: this.search.value,
        status: this.statusFilter.value,
        page: this.pageIndex(),
        size: this.pageSize,
        sort: `${this.sortField()},${this.sortDir()}`,
      })
      .subscribe({
        next: (page) => {
          this.page.set(page);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(errorMessage(err));
          this.loading.set(false);
        },
      });
  }

  /** Clicking a column header sorts by it; clicking the same header again flips the direction. */
  sortBy(field: SortField): void {
    if (this.sortField() === field) {
      this.sortDir.set(this.sortDir() === 'asc' ? 'desc' : 'asc');
    } else {
      this.sortField.set(field);
      this.sortDir.set(field === 'appliedDate' ? 'desc' : 'asc');
    }
    this.reload();
  }

  sortIndicator(field: SortField): string {
    if (this.sortField() !== field) return '';
    return this.sortDir() === 'asc' ? '▲' : '▼';
  }

  goToPage(index: number): void {
    this.pageIndex.set(index);
    this.load();
  }

  /** Inline status change from the dropdown in each row (PATCH /api/applications/{id}/status). */
  changeStatus(app: JobApplication, status: string): void {
    this.api.updateStatus(app.id, status as ApplicationStatus).subscribe({
      next: (updated) => {
        // Replace the one row in place instead of reloading the whole page.
        this.page.update((p) =>
          p ? { ...p, content: p.content.map((a) => (a.id === updated.id ? updated : a)) } : p,
        );
      },
      error: (err) => this.error.set(errorMessage(err)),
    });
  }

  delete(app: JobApplication): void {
    this.api.delete(app.id).subscribe({
      next: () => {
        this.confirmingDeleteId.set(null);
        // If we just deleted the last row on a page > 1, step back one page.
        const p = this.page();
        if (p && p.content.length === 1 && this.pageIndex() > 0) {
          this.pageIndex.update((i) => i - 1);
        }
        this.load();
      },
      error: (err) => this.error.set(errorMessage(err)),
    });
  }

  clearFilters(): void {
    // emitEvent: false avoids triggering two separate reloads; we reload once ourselves.
    this.search.setValue('', { emitEvent: false });
    this.statusFilter.setValue('', { emitEvent: false });
    this.reload();
  }
}
