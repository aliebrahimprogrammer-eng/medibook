
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, forkJoin } from 'rxjs';
import { map } from 'rxjs/operators';
import { API_ENDPOINTS } from '../core/api.config';

export type AuditLogSource = 'USER' | 'BUSINESS';

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
  source: AuditLogSource;
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

  getAuditLogs(
    page = 0,
    size = 20,
    search = '',
  ): Observable<AuditLogPage> {
    const pages = Array.from({ length: page + 1 }, (_, index) => index);

    const userRequests = pages.map((currentPage) =>
      this.getPage(
        API_ENDPOINTS.userAuditLogs,
        currentPage,
        size,
        search,
      ).pipe(
        map((result) => ({
          ...result,
          content: (result.content ?? []).map((log) => ({
            ...log,
            source: 'USER' as const,
          })),
        })),
      ),
    );

    const businessRequests = pages.map((currentPage) =>
      this.getPage(
        API_ENDPOINTS.businessAuditLogs,
        currentPage,
        size,
        search,
      ).pipe(
        map((result) => ({
          ...result,
          content: (result.content ?? []).map((log) => ({
            ...log,
            source: 'BUSINESS' as const,
          })),
        })),
      ),
    );

    return forkJoin({
      user: forkJoin(userRequests),
      business: forkJoin(businessRequests),
    }).pipe(
      map(({ user, business }) => {
        const totalElements =
          (user[0]?.totalElements ?? 0) +
          (business[0]?.totalElements ?? 0);

        const combinedLogs = [
          ...user.flatMap((result) => result.content),
          ...business.flatMap((result) => result.content),
        ];

        combinedLogs.sort((a, b) => {
          const dateDifference =
            new Date(b.createdAt).getTime() -
            new Date(a.createdAt).getTime();

          if (dateDifference !== 0) {
            return dateDifference;
          }

          return (
            a.source.localeCompare(b.source) ||
            a.id - b.id
          );
        });

        const content = combinedLogs.slice(
          page * size,
          (page + 1) * size,
        );

        const totalPages = Math.ceil(totalElements / size);

        return {
          content,
          totalPages,
          totalElements,
          number: page,
          size,
          first: page === 0,
          last: page + 1 >= totalPages,
          empty: content.length === 0,
        };
      }),
    );
  }

  private getPage(
    url: string,
    page: number,
    size: number,
    search: string,
  ) {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', 'createdAt,desc');

    if (search.trim()) {
      params = params.set('search', search.trim());
    }

    return this.http.get<{
      content: Omit<AuditLog, 'source'>[];
      totalPages: number;
      totalElements: number;
      number: number;
      size: number;
      first: boolean;
      last: boolean;
      empty: boolean;
    }>(url, { params });
  }
}
