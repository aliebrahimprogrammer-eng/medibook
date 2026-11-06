
import { CommonModule } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import {
  AdminUser,
  AdminUserService,
} from './services/admin-user.service';

interface Specialization {
  id: number;
  name: string;
  description?: string;
}

interface DoctorResponse {
  id: number;
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  specializationId: number;
  specializationName: string;
  licenseNumber: string;
  bio?: string;
}

interface DoctorPage {
  content: DoctorResponse[];
  totalElements: number;
}

@Component({
  selector: 'app-admin-create-doctor',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin-create-doctor.component.html',
  styleUrl: './admin-create-doctor.component.css',
})
export class AdminCreateDoctorComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly adminUserService = inject(AdminUserService);

  private readonly specializationsUrl =
    'http://localhost:8080/api/specializations';

  private readonly doctorsUrl = 'http://localhost:8080/api/doctors';

  readonly isLoading = signal(true);
  readonly isSubmitting = signal(false);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');

  readonly doctorUsers = signal<AdminUser[]>([]);
  readonly specializations = signal<Specialization[]>([]);

  userId: number | null = null;
  specializationId: number | null = null;
  licenseNumber = '';
  bio = '';

  ngOnInit(): void {
    this.loadFormData();
  }

  loadFormData(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    const params = new HttpParams()
      .set('page', '0')
      .set('size', '1000')
      .set('sort', 'id,asc');

    forkJoin({
      users: this.adminUserService.getAllUsers(),
      specializations: this.http.get<Specialization[]>(
        this.specializationsUrl,
      ),
      doctors: this.http.get<DoctorPage>(this.doctorsUrl, { params }),
    }).subscribe({
      next: ({ users, specializations, doctors }) => {
        const existingDoctorUserIds = new Set(
          doctors.content.map((doctor) => doctor.userId),
        );

        this.doctorUsers.set(
          users.filter(
            (user) =>
              user.role === 'DOCTOR' &&
              user.status === 'ACTIVE' &&
              !existingDoctorUserIds.has(user.id),
          ),
        );

        this.specializations.set(specializations);

        if (this.doctorUsers().length > 0) {
          this.userId = this.doctorUsers()[0].id;
        }

        if (specializations.length > 0) {
          this.specializationId = specializations[0].id;
        }

        if (this.doctorUsers().length === 0) {
          this.errorMessage.set(
            'No eligible doctor users found. Create or promote an active user to the DOCTOR role in User Management first.',
          );
        } else if (specializations.length === 0) {
          this.errorMessage.set(
            'No specializations are available. Add a specialization before creating a doctor profile.',
          );
        }

        this.isLoading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(
          error.error?.message ??
            'Could not load doctor users and specializations. Please retry.',
        );
        this.isLoading.set(false);
      },
    });
  }

  createDoctor(): void {
    this.errorMessage.set('');
    this.successMessage.set('');

    const license = this.licenseNumber.trim();
    const trimmedBio = this.bio.trim();

    if (this.userId === null) {
      this.errorMessage.set('Please select a doctor user.');
      return;
    }

    if (this.specializationId === null) {
      this.errorMessage.set('Please select a specialization.');
      return;
    }

    if (!license) {
      this.errorMessage.set('License number is required.');
      return;
    }

    if (license.length > 100) {
      this.errorMessage.set(
        'License number must not exceed 100 characters.',
      );
      return;
    }

    if (trimmedBio.length > 2000) {
      this.errorMessage.set('Bio must not exceed 2000 characters.');
      return;
    }

    this.isSubmitting.set(true);

    this.http.post<DoctorResponse>(this.doctorsUrl, {
      userId: this.userId,
      specializationId: this.specializationId,
      licenseNumber: license,
      bio: trimmedBio || null,
    }).subscribe({
      next: (doctor) => {
        this.successMessage.set(
          `Doctor profile created successfully for ${doctor.firstName} ${doctor.lastName}.`,
        );

        this.licenseNumber = '';
        this.bio = '';

        this.isSubmitting.set(false);

        // Reload lists so the newly linked user is no longer selectable.
        this.loadFormData();
      },
      error: (error) => {
        this.errorMessage.set(
          error.error?.message ??
            'Could not create the doctor profile. Check the selected user and license number.',
        );
        this.isSubmitting.set(false);
      },
    });
  }
}
