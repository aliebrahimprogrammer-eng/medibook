import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = 'http://localhost:8080/api/auth';

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${this.apiUrl}/login`, credentials)
      .pipe(
        tap((response) => {
          sessionStorage.setItem('medibook_token', response.token);
        }),
      );
  }

  logout(): void {
    sessionStorage.removeItem('medibook_token');
  }

  getToken(): string | null {
    return sessionStorage.getItem('medibook_token');
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }
}