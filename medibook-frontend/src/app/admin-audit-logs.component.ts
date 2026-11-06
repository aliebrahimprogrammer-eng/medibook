
import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import {
  AuditLog,
  AuditLogService,
} from './services/audit-log.service';

@Component({
  selector: 'app-admin-audit-logs',
  standalone: true,
  imports: [DatePipe, FormsModule, RouterLink],
  templateUrl: './admin-audit-logs.component.html',
  styleUrl: './admin-audit-logs.component.css',
})
export class AdminAuditLogsComponent implements OnInit {
  private readonly auditLogService = inject(AuditLogService);

  logs = signal<AuditLog[]>([]);
  isLoading = signal(true);
  errorMessage = signal('');
  searchInput = '';
  appliedSearch = '';
  currentPage = signal(0);
  totalPages = signal(0);
  totalElements = signal(0);
  pageSize = 20;

  ngOnInit(): void {
    this.loadLogs(0);
  }

  loadLogs(page = this.currentPage()): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.auditLogService
      .getAuditLogs(page, this.pageSize, this.appliedSearch)
      .subscribe({
        next: (result) => {
          this.logs.set(result.content ?? []);
          this.currentPage.set(result.number ?? page);
          this.totalPages.set(result.totalPages ?? 0);
          this.totalElements.set(result.totalElements ?? 0);
          this.isLoading.set(false);
        },
        error: (error: HttpErrorResponse) => {
          console.error('Could not load audit logs:', error);
          this.errorMessage.set(this.getErrorMessage(error));
          this.isLoading.set(false);
        },
      });
  }

  search(): void {
    this.appliedSearch = this.searchInput.trim();
    this.loadLogs(0);
  }

  clearSearch(): void {
    this.searchInput = '';
    this.appliedSearch = '';
    this.loadLogs(0);
  }

  previousPage(): void {
    if (this.currentPage() > 0 && !this.isLoading()) {
      this.loadLogs(this.currentPage() - 1);
    }
  }

  nextPage(): void {
    if (
      this.currentPage() + 1 < this.totalPages() &&
      !this.isLoading()
    ) {
      this.loadLogs(this.currentPage() + 1);
    }
  }

  trackLog(_: number, log: AuditLog): number {
    return log.id;
  }

  private getErrorMessage(error: HttpErrorResponse): string {
    const message = error.error?.message;

    if (typeof message === 'string' && message.trim()) {
      return message;
    }

    if (error.status === 0) {
      return 'Cannot connect to the backend. Make sure Spring Boot is running.';
    }

    if (error.status === 401) {
      return 'Your session may have expired. Please sign in again.';
    }

    if (error.status === 403) {
      return 'Access denied. Audit logs are available to administrators only.';
    }

    return 'Could not load audit logs. Please try again.';
  }
}
