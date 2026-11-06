import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';

import {
  AppointmentService,
  AppointmentResponse,
} from './services/appointment.service';

@Component({
  selector: 'app-doctor-appointments',
  standalone: true,
  imports: [DatePipe, RouterLink],
  templateUrl: './doctor-appointments.component.html',
  styleUrl: './doctor-appointments.component.css',
})
export class DoctorAppointmentsComponent implements OnInit {
  private readonly appointmentService = inject(AppointmentService);

  // Appointment data
  appointments = signal<AppointmentResponse[]>([]);

  // Loading and messages
  isLoading = signal(true);
  errorMessage = signal('');
  successMessage = signal('');
  isUpdating = signal<number | null>(null);

  // Pagination
  currentPage = signal(0);
  totalPages = signal(0);
  totalElements = signal(0);

  ngOnInit(): void {
    this.loadAppointments();
  }

  /**
   * Loads the appointments belonging to the authenticated doctor.
   */
  loadAppointments(page = 0): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.appointmentService.getMyDoctorAppointments(page, 10).subscribe({
      next: (result) => {
        this.appointments.set(result.content ?? []);
        this.currentPage.set(result.number ?? page);
        this.totalPages.set(result.totalPages ?? 0);
        this.totalElements.set(result.totalElements ?? 0);
        this.isLoading.set(false);
      },

      error: (error) => {
        console.error('Doctor appointments error:', error);

        this.errorMessage.set(
          `Could not load appointments (HTTP ${error.status}). Please try again.`,
        );

        this.isLoading.set(false);
      },
    });
  }

  /**
   * Defines the appointment status transitions allowed by the frontend.
   *
   * PENDING   -> CONFIRMED or CANCELLED
   * CONFIRMED -> COMPLETED or CANCELLED
   * COMPLETED -> no further transitions
   * CANCELLED -> no further transitions
   */
  private canTransitionStatus(
    current: AppointmentResponse['status'],
    next: AppointmentResponse['status'],
  ): boolean {
    if (current === 'PENDING') {
      return next === 'CONFIRMED' || next === 'CANCELLED';
    }

    if (current === 'CONFIRMED') {
      return next === 'COMPLETED' || next === 'CANCELLED';
    }

    return false;
  }

  /**
   * Updates an appointment's status through the backend API.
   */
  updateStatus(
    appointment: AppointmentResponse,
    status: AppointmentResponse['status'],
  ): void {
    // Prevent invalid transitions and simultaneous updates.
    if (
      !this.canTransitionStatus(appointment.status, status) ||
      this.isUpdating() !== null
    ) {
      return;
    }

    this.isUpdating.set(appointment.id);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.appointmentService
      .updateAppointmentStatus(appointment.id, status)
      .subscribe({
        next: () => {
          this.isUpdating.set(null);

          this.successMessage.set(
            `Appointment #${appointment.id} updated to ${status}.`,
          );

          // Refresh the current page while preserving the success message.
          this.refreshAppointments(this.currentPage());
        },

        error: (error) => {
          console.error('Appointment status update failed:', error);

          this.isUpdating.set(null);

          this.errorMessage.set(
            `Could not update appointment #${appointment.id} (HTTP ${error.status}).`,
          );
        },
      });
  }

  /**
   * Refreshes the appointment list after a successful status update.
   */
  private refreshAppointments(page: number): void {
    this.appointmentService.getMyDoctorAppointments(page, 10).subscribe({
      next: (result) => {
        this.appointments.set(result.content ?? []);
        this.currentPage.set(result.number ?? page);
        this.totalPages.set(result.totalPages ?? 0);
        this.totalElements.set(result.totalElements ?? 0);
      },

      error: (error) => {
        console.error('Could not refresh appointments:', error);

        this.errorMessage.set(
          `Status was updated, but the appointment list could not refresh (HTTP ${error.status}).`,
        );
      },
    });
  }

  /**
   * Moves to the previous page.
   */
  previousPage(): void {
    if (this.currentPage() > 0 && !this.isLoading()) {
      this.loadAppointments(this.currentPage() - 1);
    }
  }

  /**
   * Moves to the next page.
   */
  nextPage(): void {
    if (
      this.currentPage() + 1 < this.totalPages() &&
      !this.isLoading()
    ) {
      this.loadAppointments(this.currentPage() + 1);
    }
  }
}