
import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import {
  AdminUserService,
  AdminUser,
  UserRole,
  UserStatus,
} from './services/admin-user.service';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin-users.component.html',
  styleUrl: './admin-users.component.css',
})
export class AdminUsersComponent implements OnInit {
  private readonly adminUserService = inject(AdminUserService);

  users = signal<AdminUser[]>([]);
  isLoading = signal(true);
  errorMessage = signal('');
  successMessage = signal('');
  searchTerm = signal('');
  roleFilter = signal('ALL');
  statusFilter = signal('ALL');
  busyUserId = signal<number | null>(null);
  roleChanges = signal<Record<number, UserRole>>({});

  readonly roles: UserRole[] = ['PATIENT', 'DOCTOR', 'ADMIN'];

  filteredUsers = computed(() => {
    const search = this.searchTerm().trim().toLowerCase();
    const role = this.roleFilter();
    const status = this.statusFilter();

    return this.users().filter((user) => {
      const matchesSearch =
        !search ||
        user.email.toLowerCase().includes(search) ||
        String(user.id).includes(search);

      const matchesRole = role === 'ALL' || user.role === role;
      const matchesStatus = status === 'ALL' || user.status === status;

      return matchesSearch && matchesRole && matchesStatus;
    });
  });

  activeCount = computed(
    () => this.users().filter((user) => user.status === 'ACTIVE').length,
  );

  inactiveCount = computed(
    () => this.users().filter((user) => user.status === 'INACTIVE').length,
  );

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.adminUserService.getAllUsers().subscribe({
      next: (users) => {
        this.users.set(users);
        this.roleChanges.set(
          Object.fromEntries(users.map((user) => [user.id, user.role])),
        );
        this.isLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        console.error('Could not load users:', error);
        this.errorMessage.set(this.getErrorMessage(error));
        this.isLoading.set(false);
      },
    });
  }

  setRoleFilter(value: string): void {
    this.roleFilter.set(value);
  }

  setStatusFilter(value: string): void {
    this.statusFilter.set(value);
  }

  setSearchTerm(value: string): void {
    this.searchTerm.set(value);
  }

  setRoleChange(id: number, role: UserRole): void {
    this.roleChanges.update((changes) => ({
      ...changes,
      [id]: role,
    }));
  }

  saveRole(user: AdminUser): void {
    const newRole = this.roleChanges()[user.id];

    if (!newRole || newRole === user.role || this.busyUserId() !== null) {
      return;
    }

    const confirmed = window.confirm(
      `Change ${user.email}'s role from ${user.role} to ${newRole}?`,
    );

    if (!confirmed) {
      this.resetRole(user);
      return;
    }

    this.beginAction(user.id);

    this.adminUserService.updateUserRole(user.id, newRole).subscribe({
      next: (updatedUser) => {
        this.users.update((users) =>
          users.map((item) => item.id === updatedUser.id ? updatedUser : item),
        );
        this.roleChanges.update((changes) => ({
          ...changes,
          [updatedUser.id]: updatedUser.role,
        }));
        this.finishAction(
          `Role updated successfully for ${updatedUser.email}.`,
        );
      },
      error: (error: HttpErrorResponse) => {
        this.finishAction(
          '',
          this.getErrorMessage(error, 'Could not update the user role.'),
        );
        this.resetRole(user);
      },
    });
  }

  resetRole(user: AdminUser): void {
    this.setRoleChange(user.id, user.role);
  }

  toggleStatus(user: AdminUser): void {
    if (this.busyUserId() !== null) {
      return;
    }

    const isActive = user.status === 'ACTIVE';
    const action = isActive ? 'deactivate' : 'reactivate';

    const confirmed = window.confirm(
      `Are you sure you want to ${action} ${user.email}'s account?`,
    );

    if (!confirmed) {
      return;
    }

    this.beginAction(user.id);

    const request = isActive
      ? this.adminUserService.deactivateUser(user.id)
      : this.adminUserService.reactivateUser(user.id);

    request.subscribe({
      next: () => {
        const newStatus: UserStatus = isActive ? 'INACTIVE' : 'ACTIVE';

        this.users.update((users) =>
          users.map((item) =>
            item.id === user.id ? { ...item, status: newStatus } : item,
          ),
        );

        this.finishAction(
          `Account ${isActive ? 'deactivated' : 'reactivated'} successfully.`,
        );
      },
      error: (error: HttpErrorResponse) => {
        this.finishAction(
          '',
          this.getErrorMessage(error, `Could not ${action} this account.`),
        );
      },
    });
  }

  private beginAction(userId: number): void {
    this.busyUserId.set(userId);
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  private finishAction(success: string, error = ''): void {
    this.busyUserId.set(null);
    this.successMessage.set(success);
    this.errorMessage.set(error);
  }

  private getErrorMessage(
    error: HttpErrorResponse,
    fallback = 'Could not load users. Please try again.',
  ): string {
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
      return 'Access denied. Please sign in with an administrator account.';
    }

    return fallback;
  }
}
