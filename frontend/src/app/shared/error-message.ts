import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../core/models';

/**
 * Turns any HTTP error into a sentence that can be shown to the user.
 * Uses the backend's ApiError body when present, otherwise falls back to generic text.
 */
export function errorMessage(err: unknown, fallback = 'Something went wrong. Please try again.'): string {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) {
      return 'Cannot reach the server. Is the backend running?';
    }
    const body = err.error as Partial<ApiError> | null;
    if (body?.fieldErrors && Object.keys(body.fieldErrors).length > 0) {
      return Object.entries(body.fieldErrors)
        .map(([field, msg]) => `${field} ${msg}`)
        .join('; ');
    }
    if (body?.message) {
      return body.message;
    }
  }
  return fallback;
}
