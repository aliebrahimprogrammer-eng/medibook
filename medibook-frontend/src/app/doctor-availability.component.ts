
import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { DoctorService, Doctor } from './services/doctor.service';
import {
  Availability,
  AvailabilityService,
} from './services/availability.service';

@Component({
  selector: 'app-doctor-availability',
  standalone: true,
  imports: [DatePipe, FormsModule, RouterLink],
  templateUrl: './doctor-availability.component.html',
  styleUrl: './doctor-availability.component.css',
})
export class DoctorAvailabilityComponent implements OnInit {
  private readonly doctorService = inject(DoctorService);
  private readonly availabilityService = inject(AvailabilityService);

  doctor = signal<Doctor | null>(null);
  availabilities = signal<Availability[]>([]);

  isLoading = signal(true);
  isCreating = signal(false);
  deletingId = signal<number | null>(null);

  errorMessage = signal('');
  successMessage = signal('');

  currentPage = signal(0);
  totalPages = signal(0);
  totalElements = signal(0);

  startDateTime = '';
  endDateTime = '';

  ngOnInit(): void {
    this.loadDoctorProfile();
  }

  private loadDoctorProfile(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.doctorService.getMyDoctorProfile().subscribe({
      next: (doctor) => {
        this.doctor.set(doctor);
        this.loadAvailability(0);
      },
      error: (error) => {
        console.error('Could not load doctor profile:', error);

        this.errorMessage.set(
          `Could not load your doctor profile (HTTP ${error.status}). Please try again.`,
        );

        this.isLoading.set(false);
      },
    });
  }

  retryLoading(): void {
    if (this.doctor()) {
      this.loadAvailability(this.currentPage());
    } else {
      this.loadDoctorProfile();
    }
  }

  loadAvailability(page = 0): void {
    const doctor = this.doctor();

    if (!doctor) {
      this.isLoading.set(false);
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');

    this.availabilityService
      .getDoctorAvailability(doctor.id, page, 10)
      .subscribe({
        next: (result) => {
          this.availabilities.set(result.content ?? []);
          this.currentPage.set(result.number ?? page);
          this.totalPages.set(result.totalPages ?? 0);
          this.totalElements.set(result.totalElements ?? 0);
          this.isLoading.set(false);
        },
        error: (error) => {
          console.error('Could not load availability:', error);

          this.errorMessage.set(
            `Could not load availability (HTTP ${error.status}). Please try again.`,
          );

          this.isLoading.set(false);
        },
      });
  }

  createAvailability(): void {
    const doctor = this.doctor();

    if (!doctor || this.isCreating()) {
      return;
    }

    this.errorMessage.set('');
    this.successMessage.set('');

    if (!this.startDateTime || !this.endDateTime) {
      this.errorMessage.set(
        'Please select both a start and end date/time.',
      );
      return;
    }

    const start = new Date(this.startDateTime);
    const end = new Date(this.endDateTime);

    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
      this.errorMessage.set('Please enter valid dates and times.');
      return;
    }

    if (start.getTime() <= Date.now()) {
      this.errorMessage.set(
        'The start date and time must be in the future.',
      );
      return;
    }

    if (end.getTime() <= start.getTime()) {
      this.errorMessage.set(
        'The end time must be after the start time.',
      );
      return;
    }

    this.isCreating.set(true);

    this.availabilityService
      .createAvailability({
        doctorId: doctor.id,
        startDateTime: this.startDateTime,
        endDateTime: this.endDateTime,
      })
      .subscribe({
        next: () => {
          this.isCreating.set(false);
          this.startDateTime = '';
          this.endDateTime = '';
          this.successMessage.set(
            'Availability created successfully.',
          );

          this.loadAvailability(0);
        },
        error: (error) => {
          console.error('Could not create availability:', error);
          this.isCreating.set(false);

          this.errorMessage.set(
            this.getErrorMessage(
              error,
              'Could not create availability. Please check the selected times.',
            ),
          );
        },
      });
  }

  deleteAvailability(availability: Availability): void {
    if (this.deletingId() !== null) {
      return;
    }

    const confirmed = window.confirm(
      `Delete availability slot #${availability.id}?\n\n` +
        `${new Date(availability.startDateTime).toLocaleString()} – ` +
        `${new Date(availability.endDateTime).toLocaleString()}`,
    );

    if (!confirmed) {
      return;
    }

    this.errorMessage.set('');
    this.successMessage.set('');
    this.deletingId.set(availability.id);

    this.availabilityService
      .deleteAvailability(availability.id)
      .subscribe({
        next: () => {
          this.deletingId.set(null);
          this.successMessage.set(
            'Availability deleted successfully.',
          );

          const pageToLoad =
            this.availabilities().length === 1 &&
            this.currentPage() > 0
              ? this.currentPage() - 1
              : this.currentPage();

          this.loadAvailability(pageToLoad);
        },
        error: (error) => {
          console.error('Could not delete availability:', error);
          this.deletingId.set(null);

          this.errorMessage.set(
            this.getErrorMessage(
              error,
              'Could not delete this availability slot.',
            ),
          );
        },
      });
  }

  previousPage(): void {
    if (this.currentPage() > 0 && !this.isLoading()) {
      this.loadAvailability(this.currentPage() - 1);
    }
  }

  nextPage(): void {
    if (
      this.currentPage() + 1 < this.totalPages() &&
      !this.isLoading()
    ) {
      this.loadAvailability(this.currentPage() + 1);
    }
  }

  private getErrorMessage(error: any, fallback: string): string {
    const message = error?.error?.message;

    if (typeof message === 'string' && message.trim()) {
      return message;
    }

    const detail = error?.error?.detail;

    if (typeof detail === 'string' && detail.trim()) {
      return detail;
    }

    if (error?.status === 0) {
      return 'Cannot connect to the backend. Make sure Spring Boot is running.';
    }

    if (error?.status === 401) {
      return 'Your session may have expired. Please log in again.';
    }

    if (error?.status === 403) {
      return 'You are not authorized to perform this action.';
    }

    if (error?.status === 409) {
      return 'This time overlaps with existing availability. Please choose another time.';
    }

    return fallback;
  }
}
