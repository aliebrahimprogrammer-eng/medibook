
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AuditLog {
  id: number;
  userId: number;
  userEmail: string;
  userRole: string;
  action: string;
  entityType: string;
  entityId: number | null;
  description: string;
  createdAt: string;
}

export interface AuditLogPage {
  content: AuditLog[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class AuditLogService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = 'http://localhost:8080/api/audit-logs';

  getAuditLogs(
    page = 0,
    size = 20,
    search = '',
  ): Observable<AuditLogPage> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', 'createdAt,desc');

    if (search.trim()) {
      params = params.set('search', search.trim());
    }

    return this.http.get<AuditLogPage>(this.apiUrl, { params });
  }
}
