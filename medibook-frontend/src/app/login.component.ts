
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { PasswordModule } from 'primeng/password';

import { AuthService } from './services/auth.service';
import { ProfileService, UserRole } from './services/profile.service';

function dashboardPath(role: UserRole): string {
  switch (role) {
    case 'PATIENT':
      return '/patient';
    case 'DOCTOR':
      return '/doctor';
    case 'ADMIN':
      return '/admin';
  }
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, InputTextModule, ButtonModule, PasswordModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent {
  private readonly authService = inject(AuthService);
  private readonly profileService = inject(ProfileService);
  private readonly router = inject(Router);

  email = '';
  password = '';
  errorMessage = '';
  successMessage = '';
  isLoading = false;

  onLogin(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.email.trim() || !this.password) {
      this.errorMessage = 'Please enter your email and password.';
      return;
    }

    this.isLoading = true;

    this.authService.login({
      email: this.email.trim(),
      password: this.password,
    }).subscribe({
      next: () => {
        this.profileService.getMyProfile().subscribe({
          next: (profile) => {
            this.isLoading = false;

            const destination = dashboardPath(profile.role);
            void this.router.navigateByUrl(destination);
          },
          error: (error) => {
            this.isLoading = false;
            this.authService.logout();

            this.errorMessage =
              error.status === 0
                ? 'Cannot reach your profile. Check the backend connection.'
                : 'Login succeeded, but we could not load your profile.';
          },
        });
      },
      error: (error) => {
        this.isLoading = false;

        if (error.status === 0) {
          this.errorMessage = 'Cannot reach the backend.';
        } else if (error.status === 401) {
          this.errorMessage = 'Invalid email or password.';
        } else if (error.status === 403) {
          this.errorMessage =
            'Access denied. Your account may need email verification.';
        } else {
          this.errorMessage =
            error.error?.message || 'Login failed. Please try again.';
        }
      },
    });
  }
}
