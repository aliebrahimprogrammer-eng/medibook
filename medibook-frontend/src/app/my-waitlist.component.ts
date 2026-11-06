
import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import {
  WaitlistEntry,
  WaitlistService,
} from './services/waitlist.service';

@Component({
  selector: 'app-my-waitlist',
  standalone: true,
  imports: [DatePipe, RouterLink],
  templateUrl: './my-waitlist.component.html',
  styleUrl: './my-waitlist.component.css',
})
export class MyWaitlistComponent implements OnInit {
  private readonly waitlistService = inject(WaitlistService);

  entries = signal<WaitlistEntry[]>([]);
  isLoading = signal(true);
  errorMessage = signal('');
  successMessage = signal('');
  cancellingEntryId = signal<number | null>(null);

  ngOnInit(): void {
    this.loadWaitlist();
  }

  loadWaitlist(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.waitlistService.getMyWaitlist().subscribe({
      next: (entries) => {
        this.entries.set(entries);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Failed to load waitlist:', error);

        this.errorMessage.set(
          error.error?.message ||
            'Could not load your waitlist. Please try again.',
        );

        this.isLoading.set(false);
      },
    });
  }

  cancelEntry(entry: WaitlistEntry): void {
    if (
      entry.status !== 'WAITING' ||
      this.cancellingEntryId() !== null
    ) {
      return;
    }

    this.cancellingEntryId.set(entry.id);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.waitlistService.cancelWaitlistEntry(entry.id).subscribe({
      next: () => {
        this.entries.update((entries) =>
          entries.map((item) =>
            item.id === entry.id
              ? { ...item, status: 'CANCELLED' }
              : item,
          ),
        );

        this.successMessage.set(
          `Waitlist request #${entry.id} was cancelled successfully.`,
        );

        this.cancellingEntryId.set(null);
      },
      error: (error) => {
        console.error('Failed to cancel waitlist entry:', error);

        this.errorMessage.set(
          error.error?.message ||
            'Could not cancel this waitlist request. Please try again.',
        );

        this.cancellingEntryId.set(null);
      },
    });
  }
}
