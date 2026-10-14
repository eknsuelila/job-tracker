import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { Chart, registerables } from 'chart.js';

import { JobApplicationService } from '../../core/api/job-application.service';
import { APPLICATION_STATUSES, JobApplication, Stats } from '../../core/models';
import { STATUS_LABELS, StatusLabelPipe } from '../../shared/status-label.pipe';
import { errorMessage } from '../../shared/error-message';

// Chart.js is tree-shakable: we must register the chart types/scales we use. registerables = all of them.
Chart.register(...registerables);

/** Chart colors per status, matching the .badge-* colors in styles.scss. */
const STATUS_COLORS: Record<string, string> = {
  WISHLIST: '#94a3b8',
  APPLIED: '#0ea5e9',
  PHONE_SCREEN: '#8b5cf6',
  INTERVIEW: '#f59e0b',
  OFFER: '#22c55e',
  REJECTED: '#ef4444',
  WITHDRAWN: '#9ca3af',
};

/**
 * Dashboard page: summary cards, two charts and the five most recent applications.
 *
 * Lifecycle hooks used:
 *  - ngOnInit:        component created -> start loading data
 *  - ngAfterViewInit: template rendered -> the <canvas> elements now exist for Chart.js
 *  - ngOnDestroy:     component removed -> destroy charts to free memory
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, DatePipe, StatusLabelPipe],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly api = inject(JobApplicationService);

  // @ViewChild gives the component a reference to an element marked with #name in the template.
  @ViewChild('statusChart') statusCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('weeklyChart') weeklyCanvas?: ElementRef<HTMLCanvasElement>;

  readonly stats = signal<Stats | null>(null);
  readonly recent = signal<JobApplication[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  // Each chart has a different generic type (Chart<"bar">, Chart<"doughnut">); we only need destroy().
  private charts: Pick<Chart, 'destroy'>[] = [];
  private viewReady = false;

  ngOnInit(): void {
    // forkJoin runs both requests in parallel and emits once BOTH have completed.
    forkJoin({
      stats: this.api.stats(),
      recent: this.api.list({ page: 0, size: 5, sort: 'appliedDate,desc' }),
    }).subscribe({
      next: ({ stats, recent }) => {
        this.stats.set(stats);
        this.recent.set(recent.content);
        this.loading.set(false);
        this.renderCharts();
      },
      error: (err) => {
        this.error.set(errorMessage(err));
        this.loading.set(false);
      },
    });
  }

  ngAfterViewInit(): void {
    this.viewReady = true;
    this.renderCharts();
  }

  ngOnDestroy(): void {
    this.charts.forEach((c) => c.destroy());
  }

  /** Count for one status, used by the summary cards. */
  count(status: keyof Stats['byStatus']): number {
    return this.stats()?.byStatus[status] ?? 0;
  }

  /**
   * Draws the charts once BOTH the data has arrived AND the canvases exist.
   * Either event can happen first, so both call this method; the guard makes it safe.
   * setTimeout waits one tick so the @if blocks have rendered the canvases into the DOM.
   */
  private renderCharts(): void {
    const stats = this.stats();
    if (!stats || !this.viewReady) {
      return;
    }
    setTimeout(() => {
      this.charts.forEach((c) => c.destroy());
      this.charts = [];
      if (this.statusCanvas) {
        this.charts.push(this.buildStatusChart(this.statusCanvas.nativeElement, stats));
      }
      if (this.weeklyCanvas) {
        this.charts.push(this.buildWeeklyChart(this.weeklyCanvas.nativeElement, stats));
      }
    });
  }

  private buildStatusChart(canvas: HTMLCanvasElement, stats: Stats) {
    // Hide statuses with 0 applications so the doughnut stays readable.
    const statuses = APPLICATION_STATUSES.filter((s) => stats.byStatus[s] > 0);
    return new Chart(canvas, {
      type: 'doughnut',
      data: {
        labels: statuses.map((s) => STATUS_LABELS[s]),
        datasets: [{
          data: statuses.map((s) => stats.byStatus[s]),
          backgroundColor: statuses.map((s) => STATUS_COLORS[s]),
          borderWidth: 2,
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '62%',
        plugins: { legend: { position: 'right' } },
      },
    });
  }

  private buildWeeklyChart(canvas: HTMLCanvasElement, stats: Stats) {
    const formatter = new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric' });
    return new Chart(canvas, {
      type: 'bar',
      data: {
        // "T00:00" makes the browser parse the date in local time instead of UTC (avoids off-by-one days).
        labels: stats.perWeek.map((w) => formatter.format(new Date(w.weekStart + 'T00:00'))),
        datasets: [{
          label: 'Applications',
          data: stats.perWeek.map((w) => w.count),
          backgroundColor: '#4f46e5',
          borderRadius: 6,
          maxBarThickness: 36,
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: { y: { beginAtZero: true, ticks: { precision: 0 } }, x: { grid: { display: false } } },
      },
    });
  }
}
