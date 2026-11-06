import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import {
  Doctor,
  DoctorService,
} from './services/doctor.service';

import {
  Specialization,
  SpecializationService,
} from './services/specialization.service';

@Component({
  selector: 'app-doctors',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './doctors.component.html',
  styleUrl: './doctors.component.css',
})
export class DoctorsComponent implements OnInit {
  private readonly doctorService = inject(DoctorService);
  private readonly specializationService = inject(SpecializationService);

  doctors = signal<Doctor[]>([]);
  specializations = signal<Specialization[]>([]);

  searchName = '';
  searchSpecialization = '';

  isLoading = signal(false);
  isLoadingSpecializations = signal(false);

  errorMessage = signal('');
  specializationError = signal('');

  currentPage = signal(0);
  totalPages = signal(0);
  totalElements = signal(0);

  ngOnInit(): void {
    this.loadSpecializations();
    this.loadDoctors();
  }

  loadSpecializations(): void {
    this.isLoadingSpecializations.set(true);
    this.specializationError.set('');

    this.specializationService.getAll().subscribe({
      next: (result) => {
        this.specializations.set(result ?? []);
        this.isLoadingSpecializations.set(false);
      },
      error: (error) => {
        console.error('Could not load specializations:', error);
        this.specializationError.set(
          'Could not load specializations. You can still search by name.',
        );
        this.isLoadingSpecializations.set(false);
      },
    });
  }

  search(): void {
    this.currentPage.set(0);
    this.loadDoctors();
  }

  loadDoctors(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.doctorService.searchDoctors(
      this.searchName,
      this.searchSpecialization,
      this.currentPage(),
      8,
    ).subscribe({
      next: (result) => {
        this.doctors.set(result.content ?? []);
        this.totalPages.set(result.totalPages ?? 0);
        this.totalElements.set(result.totalElements ?? 0);
        this.isLoading.set(false);
      },
      error: (error) => {
        this.isLoading.set(false);

        this.errorMessage.set(
          error.status === 0
            ? 'Cannot connect to MediBook. Check the backend connection.'
            : `Could not load doctors (HTTP ${error.status}).`,
        );
      },
    });
  }

  previousPage(): void {
    if (this.currentPage() > 0) {
      this.currentPage.update((page) => page - 1);
      this.loadDoctors();
    }
  }

  nextPage(): void {
    if (this.currentPage() + 1 < this.totalPages()) {
      this.currentPage.update((page) => page + 1);
      this.loadDoctors();
    }
  }

  trackDoctor(_index: number, doctor: Doctor): number {
    return doctor.id;
  }
}
