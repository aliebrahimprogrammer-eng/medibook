
import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import {
  ProfileService,
  UserProfile,
} from './services/profile.service';

@Component({
  selector: 'app-my-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './my-profile.component.html',
  styleUrl: './my-profile.component.css',
})
export class MyProfileComponent implements OnInit {
  private readonly profileService = inject(ProfileService);
  private readonly router = inject(Router);

  readonly profile = signal<UserProfile | null>(null);
  readonly isLoading = signal(true);
  readonly isSaving = signal(false);
  readonly isUploading = signal(false);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly selectedFile = signal<File | null>(null);
  readonly imagePreview = signal<string | null>(null);

  firstName = '';
  lastName = '';
  phone = '';

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.profileService.getMyProfile().subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.firstName = profile.firstName ?? '';
        this.lastName = profile.lastName ?? '';
        this.phone = profile.phone ?? '';
        this.isLoading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(
          error.status === 401
            ? 'Your session has expired. Please sign in again.'
            : 'Could not load your profile. Please try again.',
        );
        this.isLoading.set(false);
      },
    });
  }

  saveProfile(): void {
    this.errorMessage.set('');
    this.successMessage.set('');

    const firstName = this.firstName.trim();
    const lastName = this.lastName.trim();
    const phone = this.phone.trim();

    if (!firstName || !lastName) {
      this.errorMessage.set('First name and last name are required.');
      return;
    }

    this.isSaving.set(true);

    this.profileService.updateMyProfile({
      firstName,
      lastName,
      phone,
    }).subscribe({
      next: (updatedProfile) => {
        this.profile.set(updatedProfile);
        this.firstName = updatedProfile.firstName ?? '';
        this.lastName = updatedProfile.lastName ?? '';
        this.phone = updatedProfile.phone ?? '';
        this.successMessage.set('Your profile has been updated.');
        this.isSaving.set(false);
      },
      error: (error) => {
        this.errorMessage.set(
          error.error?.message ?? 'Could not update your profile.',
        );
        this.isSaving.set(false);
      },
    });
  }

  onFileSelected(event: Event): void {
    this.errorMessage.set('');
    this.successMessage.set('');

    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    if (!file) {
      this.selectedFile.set(null);
      this.imagePreview.set(null);
      return;
    }

    const allowedTypes = ['image/jpeg', 'image/png'];

    if (!allowedTypes.includes(file.type)) {
      this.errorMessage.set('Choose a JPEG or PNG image.');
      input.value = '';
      this.selectedFile.set(null);
      this.imagePreview.set(null);
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      this.errorMessage.set('The image must not exceed 5 MB.');
      input.value = '';
      this.selectedFile.set(null);
      this.imagePreview.set(null);
      return;
    }

    this.selectedFile.set(file);

    const reader = new FileReader();

    reader.onload = () => {
      if (typeof reader.result === 'string') {
        this.imagePreview.set(reader.result);
      }
    };

    reader.readAsDataURL(file);
  }

  uploadPicture(): void {
    const file = this.selectedFile();

    if (!file) {
      this.errorMessage.set('Please choose an image first.');
      return;
    }

    this.errorMessage.set('');
    this.successMessage.set('');
    this.isUploading.set(true);

    this.profileService.uploadProfilePicture(file).subscribe({
      next: (updatedProfile) => {
        this.profile.set(updatedProfile);
        this.selectedFile.set(null);
        this.imagePreview.set(null);
        this.successMessage.set('Your profile picture has been uploaded.');
        this.isUploading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(
          error.error?.message ?? 'Could not upload your profile picture.',
        );
        this.isUploading.set(false);
      },
    });
  }

  get initials(): string {
    const profile = this.profile();

    const first = profile?.firstName?.charAt(0) ?? '';
    const last = profile?.lastName?.charAt(0) ?? '';

    return `${first}${last}`.toUpperCase() || 'U';
  }

  get profilePictureSrc(): string | null {
    const filename = this.profile()?.profilePictureUrl;

    if (!filename) {
      return null;
    }

    if (/^https?:\/\//i.test(filename)) {
      return filename;
    }

    // Assumes the backend serves uploaded files from /uploads/.
    // Adjust this path if your Spring Boot resource mapping is different.
    return `http://localhost:8080/uploads/${encodeURIComponent(filename)}`;
  }

  goBack(): void {
    const role = this.profile()?.role;

    if (role === 'ADMIN') {
      void this.router.navigate(['/admin']);
    } else if (role === 'DOCTOR') {
      void this.router.navigate(['/doctor']);
    } else {
      void this.router.navigate(['/patient']);
    }
  }
}
