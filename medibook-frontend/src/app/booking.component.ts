
import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';

import {
  Availability,
  AvailabilityService,
} from './services/availability.service';

import {
  AppointmentService,
  AppointmentResponse,
} from './services/appointment.service';

import {
  DoctorService,
  Doctor,
} from './services/doctor.service';

import { WaitlistService } from './services/waitlist.service';

interface AppointmentSlot {
  id: string;
  startDateTime: string;
  endDateTime: string;
}

@Component({
  selector: 'app-booking',
  standalone: true,
  imports: [DatePipe, RouterLink, FormsModule],
  templateUrl: './booking.component.html',
  styleUrl: './booking.component.css',
})
export class BookingComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly doctorService = inject(DoctorService);
  private readonly availabilityService = inject(AvailabilityService);
  private readonly appointmentService = inject(AppointmentService);
  private readonly waitlistService = inject(WaitlistService);

  doctor = signal<Doctor | null>(null);
  appointmentSlots = signal<AppointmentSlot[]>([]);
  availableDates = signal<string[]>([]);

  selectedDate = '';
  selectedSlotId = '';
  reason = '';

  isLoading = signal(true);
  isBooking = signal(false);
  isJoiningWaitlist = signal(false);

  errorMessage = signal('');
  successMessage = signal('');
  showWaitlistPrompt = signal(false);

  ngOnInit(): void {
    const doctorId = Number(
      this.route.snapshot.paramMap.get('doctorId'),
    );

    if (!Number.isInteger(doctorId) || doctorId <= 0) {
      this.errorMessage.set('Invalid doctor ID.');
      this.isLoading.set(false);
      return;
    }

    this.doctorService.getDoctorById(doctorId).subscribe({
      next: (doctor) => this.doctor.set(doctor),
      error: () => {
        this.errorMessage.set('Could not load doctor details.');
      },
    });

    this.availabilityService
      .getDoctorAvailability(doctorId)
      .subscribe({
        next: (result) => {
          const now = Date.now();
          const generatedSlots: AppointmentSlot[] = [];

          for (const availability of result.content ?? []) {
            this.generateSlots(
              availability,
              now,
              generatedSlots,
            );
          }

          generatedSlots.sort(
            (a, b) =>
              new Date(a.startDateTime).getTime() -
              new Date(b.startDateTime).getTime(),
          );

          this.appointmentSlots.set(generatedSlots);

          const dates = [
            ...new Set(
              generatedSlots.map((slot) =>
                this.getDateKey(slot.startDateTime),
              ),
            ),
          ].sort();

          this.availableDates.set(dates);
          this.isLoading.set(false);
        },
        error: (error) => {
          console.error('Availability error:', error);

          this.errorMessage.set(
            'Could not load availability. Please try again.',
          );

          this.isLoading.set(false);
        },
      });
  }

  private generateSlots(
    availability: Availability,
    now: number,
    output: AppointmentSlot[],
  ): void {
    // These values are LocalDateTime strings without a timezone.
    // Parse them as browser-local wall-clock times, then keep the
    // generated slot strings timezone-free when sending to Java.
    const availabilityStart = new Date(
      availability.startDateTime,
    ).getTime();

    const availabilityEnd = new Date(
      availability.endDateTime,
    ).getTime();

    const duration = 30 * 60 * 1000;

    if (
      !Number.isFinite(availabilityStart) ||
      !Number.isFinite(availabilityEnd) ||
      availabilityEnd <= availabilityStart
    ) {
      return;
    }

    let start = availabilityStart;

    while (start + duration <= availabilityEnd) {
      const end = start + duration;

      if (start > now) {
        output.push({
          id: `${availability.id}-${start}`,
          startDateTime: this.toLocalDateTimeString(new Date(start)),
          endDateTime: this.toLocalDateTimeString(new Date(end)),
        });
      }

      start = end;
    }
  }

  private toLocalDateTimeString(date: Date): string {
    const pad = (value: number) => String(value).padStart(2, '0');

    return (
      `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` +
      `T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
    );
  }

  private getDateKey(dateTime: string): string {
    // The date portion is already the intended local calendar date.
    return dateTime.slice(0, 10);
  }

  onDateChange(): void {
    this.selectedSlotId = '';
    this.errorMessage.set('');
    this.successMessage.set('');
    this.showWaitlistPrompt.set(false);
  }

  onSlotChange(): void {
    this.showWaitlistPrompt.set(false);
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  slotsForSelectedDate(): AppointmentSlot[] {
    if (!this.selectedDate) {
      return [];
    }

    return this.appointmentSlots().filter(
      (slot) =>
        this.getDateKey(slot.startDateTime) === this.selectedDate,
    );
  }

  bookAppointment(): void {
    const doctor = this.doctor();

    const slot = this.slotsForSelectedDate().find(
      (item) => item.id === this.selectedSlotId,
    );

    if (!doctor || !slot) {
      this.errorMessage.set(
        'Please select a date and an available time slot.',
      );
      return;
    }

    if (this.reason.length > 500) {
      this.errorMessage.set(
        'The reason must be 500 characters or fewer.',
      );
      return;
    }

    this.isBooking.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.showWaitlistPrompt.set(false);

    this.appointmentService
      .createAppointment({
        doctorId: doctor.id,
        startDateTime: slot.startDateTime,
        endDateTime: slot.endDateTime,
        reason: this.reason.trim() || undefined,
      })
      .subscribe({
        next: (appointment: AppointmentResponse) => {
          this.isBooking.set(false);

          this.successMessage.set(
            `Appointment booked successfully! Reference: ${appointment.id}`,
          );

          this.appointmentSlots.update((slots) =>
            slots.filter((item) => item.id !== slot.id),
          );

          this.selectedSlotId = '';
          this.reason = '';

          const remainingDates = [
            ...new Set(
              this.appointmentSlots().map((item) =>
                this.getDateKey(item.startDateTime),
              ),
            ),
          ].sort();

          this.availableDates.set(remainingDates);

          if (!remainingDates.includes(this.selectedDate)) {
            this.selectedDate = '';
          }
        },

        error: (error) => {
          this.isBooking.set(false);

          console.error('Booking error:', error);

          const backendMessage = error.error?.message;

          if (
            error.status === 409 &&
            backendMessage ===
              'Doctor already has an appointment during this time'
          ) {
            this.errorMessage.set('');
            this.successMessage.set('');
            this.showWaitlistPrompt.set(true);
            return;
          }

          this.showWaitlistPrompt.set(false);

          this.errorMessage.set(
            error.status === 409
              ? backendMessage ||
                  'The appointment could not be booked because of a conflict.'
              : `Booking failed (HTTP ${error.status}). Please try again.`,
          );
        },
      });
  }

  joinWaitlist(): void {
    const doctor = this.doctor();

    const slot = this.slotsForSelectedDate().find(
      (item) => item.id === this.selectedSlotId,
    );

    if (!doctor || !slot || this.isJoiningWaitlist()) {
      return;
    }

    this.isJoiningWaitlist.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.waitlistService
      .joinWaitlist({
        doctorId: doctor.id,
        preferredStartDateTime: slot.startDateTime,
        preferredEndDateTime: slot.endDateTime,
      })
      .subscribe({
        next: () => {
          this.isJoiningWaitlist.set(false);
          this.showWaitlistPrompt.set(false);

          this.successMessage.set(
            'You have successfully joined the waitlist for your selected appointment time.',
          );

          this.appointmentSlots.update((slots) =>
            slots.filter((item) => item.id !== slot.id),
          );

          const remainingDates = [
            ...new Set(
              this.appointmentSlots().map((item) =>
                this.getDateKey(item.startDateTime),
              ),
            ),
          ].sort();

          this.availableDates.set(remainingDates);

          if (!remainingDates.includes(this.selectedDate)) {
            this.selectedDate = '';
          }

          this.selectedSlotId = '';
        },

        error: (error) => {
          console.error('Failed to join waitlist:', error);

          this.isJoiningWaitlist.set(false);

          this.errorMessage.set(
            error.error?.message ||
              'Could not join the waitlist. Please try again.',
          );
        },
      });
  }

  dismissWaitlistPrompt(): void {
    this.showWaitlistPrompt.set(false);
    this.errorMessage.set('');
  }
}
