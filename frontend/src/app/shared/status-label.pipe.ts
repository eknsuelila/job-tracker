import { Pipe, PipeTransform } from '@angular/core';
import { ApplicationStatus } from '../core/models';

/** Human-readable labels for the status enum values. */
export const STATUS_LABELS: Record<ApplicationStatus, string> = {
  WISHLIST: 'Wishlist',
  APPLIED: 'Applied',
  PHONE_SCREEN: 'Phone Screen',
  INTERVIEW: 'Interview',
  OFFER: 'Offer',
  REJECTED: 'Rejected',
  WITHDRAWN: 'Withdrawn',
};

/**
 * A pipe transforms a value inside a template: {{ app.status | statusLabel }} -> "Phone Screen".
 * Pipes are "pure" by default, so Angular only recomputes when the input value changes.
 */
@Pipe({ name: 'statusLabel', standalone: true })
export class StatusLabelPipe implements PipeTransform {
  transform(status: ApplicationStatus): string {
    return STATUS_LABELS[status] ?? status;
  }
}
