import { StatusLabelPipe } from './status-label.pipe';

// Pipes are plain classes, so they can be tested without any Angular TestBed setup.
describe('StatusLabelPipe', () => {
  const pipe = new StatusLabelPipe();

  it('turns enum values into readable labels', () => {
    expect(pipe.transform('PHONE_SCREEN')).toBe('Phone Screen');
    expect(pipe.transform('OFFER')).toBe('Offer');
  });
});
