
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_ENDPOINTS } from '../core/api.config';

export type UserRole = 'PATIENT' | 'DOCTOR' | 'ADMIN';
export type UserStatus = 'ACTIVE' | 'INACTIVE';

export interface AdminUser {
  id: number;
  email: string;
  role: UserRole;
  status: UserStatus;
  emailVerified: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class AdminUserService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = API_ENDPOINTS.adminUsers;

  getAllUsers(): Observable<AdminUser[]> {
    return this.http.get<AdminUser[]>(this.apiUrl);
  }

  updateUserRole(id: number, role: UserRole): Observable<AdminUser> {
    return this.http.patch<AdminUser>(
      `${this.apiUrl}/${id}/role`,
      { role },
    );
  }

  deactivateUser(id: number): Observable<void> {
    return this.http.patch<void>(
      `${this.apiUrl}/${id}/deactivate`,
      {},
    );
  }

  reactivateUser(id: number): Observable<void> {
    return this.http.patch<void>(
      `${this.apiUrl}/${id}/reactivate`,
      {},
    );
  }
}
