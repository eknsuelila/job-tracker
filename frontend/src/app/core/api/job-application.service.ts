import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ApplicationStatus,
  JobApplication,
  JobApplicationRequest,
  Page,
  Stats,
} from '../models';

/** Query options for the list endpoint. All optional. */
export interface ApplicationQuery {
  status?: ApplicationStatus | '';
  q?: string;
  page?: number;
  size?: number;
  sort?: string; // e.g. "appliedDate,desc" (Spring Data format: property,direction)
}

/**
 * Thin wrapper around the /api/applications REST endpoints.
 *
 * Components never build URLs themselves; they call these typed methods. HttpClient returns
 * cold Observables: nothing is sent until someone calls `.subscribe()` (or uses the async pipe).
 *
 * URLs are relative ("/api/..."). In development the Angular dev server proxies them to
 * http://localhost:8080 (see proxy.conf.json); in Docker, nginx does the same job.
 */
@Injectable({ providedIn: 'root' })
export class JobApplicationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/applications';

  list(query: ApplicationQuery = {}): Observable<Page<JobApplication>> {
    // HttpParams builds "?status=...&q=..." safely (URL-encoding included). Empty values are skipped.
    let params = new HttpParams();
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return this.http.get<Page<JobApplication>>(this.baseUrl, { params });
  }

  get(id: number): Observable<JobApplication> {
    return this.http.get<JobApplication>(`${this.baseUrl}/${id}`);
  }

  create(body: JobApplicationRequest): Observable<JobApplication> {
    return this.http.post<JobApplication>(this.baseUrl, body);
  }

  update(id: number, body: JobApplicationRequest): Observable<JobApplication> {
    return this.http.put<JobApplication>(`${this.baseUrl}/${id}`, body);
  }

  updateStatus(id: number, status: ApplicationStatus): Observable<JobApplication> {
    return this.http.patch<JobApplication>(`${this.baseUrl}/${id}/status`, { status });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  stats(): Observable<Stats> {
    return this.http.get<Stats>(`${this.baseUrl}/stats`);
  }
}
