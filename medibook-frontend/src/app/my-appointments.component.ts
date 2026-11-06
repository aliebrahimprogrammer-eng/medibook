
import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';

import {
  MyAppointmentsService,
  PatientAppointment,
} from './services/my-appointments.service';

@Component({
  selector: 'app-my-appointments',
  standalone: true,
  imports: [DatePipe, RouterLink],
  templateUrl: './my-appointments.component.html',
  styleUrl: './my-appointments.component.css',
})
export class MyAppointmentsComponent implements OnInit {
  private readonly appointmentsService = inject(MyAppointmentsService);

  appointments = signal<PatientAppointment[]>([]);
  isLoading = signal(true);
  isCancelling = signal<number | null>(null);
  errorMessage = signal('');
  successMessage = signal('');

  currentPage = signal(0);
  totalPages = signal(0);
  totalElements = signal(0);

  ngOnInit(): void {
    this.loadAppointments();
  }

  loadAppointments(page = 0): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.appointmentsService.getMyAppointments(page, 10).subscribe({
      next: (result) => {
        this.appointments.set(result.content ?? []);
        this.currentPage.set(result.number ?? page);
        this.totalPages.set(result.totalPages ?? 0);
        this.totalElements.set(result.totalElements ?? 0);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Could not load appointments:', error);
        this.errorMessage.set(
          `Could not load your appointments (HTTP ${error.status}). Please try again.`,
        );
        this.isLoading.set(false);
      },
    });
  }

  cancelAppointment(appointment: PatientAppointment): void {
    if (
      !confirm(
        `Are you sure you want to cancel your appointment with ${appointment.doctorName}?`,
      )
    ) {
      return;
    }

    this.isCancelling.set(appointment.id);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.appointmentsService.cancelAppointment(appointment.id).subscribe({
      next: () => {
        this.isCancelling.set(null);
        this.successMessage.set(
          `Appointment #${appointment.id} cancelled successfully.`,
        );
        this.loadAppointments(this.currentPage());
      },
      error: (error) => {
        console.error('Could not cancel appointment:', error);
        this.isCancelling.set(null);
        this.errorMessage.set(
          `Could not cancel appointment (HTTP ${error.status}). Please try again.`,
        );
      },
    });
  }

  canCancel(appointment: PatientAppointment): boolean {
    return (
      appointment.status === 'PENDING' ||
      appointment.status === 'CONFIRMED'
    );
  }

  previousPage(): void {
    if (this.currentPage() > 0) {
      this.loadAppointments(this.currentPage() - 1);
    }
  }

  nextPage(): void {
    if (this.currentPage() + 1 < this.totalPages()) {
      this.loadAppointments(this.currentPage() + 1);
    }
  }
}
