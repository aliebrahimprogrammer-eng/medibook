
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { API_ENDPOINTS } from '../core/api.config';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
}

export interface RegisterResponse {
  id: number;
  email: string;
  role: string;
  status: string;
  emailVerified: boolean;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = API_ENDPOINTS.auth;

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${this.apiUrl}/login`, credentials)
      .pipe(
        tap((response) => {
          sessionStorage.setItem('medibook_token', response.token);
        }),
      );
  }

  register(request: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(
      `${this.apiUrl}/register`,
      request,
    );
  }

  verifyEmail(token: string): Observable<void> {
    return this.http.get<void>(`${this.apiUrl}/verify-email`, {
      params: { token },
    });
  }

  forgotPassword(request: ForgotPasswordRequest): Observable<void> {
    return this.http.post<void>(
      `${this.apiUrl}/forgot-password`,
      request,
    );
  }

  resetPassword(request: ResetPasswordRequest): Observable<void> {
    return this.http.post<void>(
      `${this.apiUrl}/reset-password`,
      request,
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
