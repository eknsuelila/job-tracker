/**
 * TypeScript interfaces that mirror the JSON returned by the Spring Boot API.
 *
 * Keeping these in one place means that if the backend DTOs change, the compiler shows
 * every frontend file that needs updating. Interfaces have no runtime cost: they are erased
 * when TypeScript compiles to JavaScript.
 */

/** Must match the Java enum `ApplicationStatus` (same names, same order). */
export type ApplicationStatus =
  | 'WISHLIST'
  | 'APPLIED'
  | 'PHONE_SCREEN'
  | 'INTERVIEW'
  | 'OFFER'
  | 'REJECTED'
  | 'WITHDRAWN';

/** All statuses in pipeline order, used for dropdowns and charts. */
export const APPLICATION_STATUSES: ApplicationStatus[] = [
  'WISHLIST', 'APPLIED', 'PHONE_SCREEN', 'INTERVIEW', 'OFFER', 'REJECTED', 'WITHDRAWN',
];

export interface User {
  id: number;
  name: string;
  email: string;
}

/** Response of POST /api/auth/login and /api/auth/register. */
export interface AuthResponse {
  token: string;
  user: User;
}

/** A job application as returned by the API. Dates arrive as ISO strings, e.g. "2026-10-14". */
export interface JobApplication {
  id: number;
  company: string;
  position: string;
  location: string | null;
  jobUrl: string | null;
  salary: number | null;
  status: ApplicationStatus;
  appliedDate: string;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

/** Body sent when creating or updating an application (no id or timestamps: the server owns those). */
export type JobApplicationRequest = Omit<JobApplication, 'id' | 'createdAt' | 'updatedAt'>;

/** Generic page of results; mirrors the Java `PageResponse<T>` record. */
export interface Page<T> {
  content: T[];
  page: number;          // zero-based page index
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface WeeklyCount {
  weekStart: string;
  count: number;
}

/** Response of GET /api/applications/stats. */
export interface Stats {
  total: number;
  byStatus: Record<ApplicationStatus, number>;
  perWeek: WeeklyCount[];
  responseRate: number;
}

/** Shape of every error body produced by the backend's GlobalExceptionHandler. */
export interface ApiError {
  status: number;
  error: string;
  message: string;
  fieldErrors: Record<string, string>;
}
