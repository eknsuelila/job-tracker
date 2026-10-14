import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let client: HttpClient;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    client = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('adds the Bearer token when logged in', () => {
    localStorage.setItem('jobtracker.token', 'my-jwt');
    client.get('/api/applications').subscribe();
    const req = http.expectOne('/api/applications');
    expect(req.request.headers.get('Authorization')).toBe('Bearer my-jwt');
    req.flush({});
  });

  it('sends no Authorization header when logged out', () => {
    client.get('/api/applications').subscribe();
    const req = http.expectOne('/api/applications');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });
});
