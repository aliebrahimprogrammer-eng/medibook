
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.css',
})
export class ForgotPasswordComponent {
  private readonly authService = inject(AuthService);

  email = '';
  errorMessage = '';
  successMessage = '';
  isLoading = false;
  requestSubmitted = false;

  onSubmit(): void {
    this.errorMessage = '';
    this.successMessage = '';

    const email = this.email.trim();

    if (!email) {
      this.errorMessage = 'Please enter your email address.';
      return;
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      this.errorMessage = 'Please enter a valid email address.';
      return;
    }

    this.isLoading = true;

    this.authService.forgotPassword({ email }).subscribe({
      next: () => {
        this.isLoading = false;
        this.requestSubmitted = true;
        this.successMessage =
          'If an account exists with this email address, you will receive a password reset email shortly.';
      },
      error: () => {
        this.isLoading = false;

        // Avoid revealing whether an email address has an account.
        this.requestSubmitted = true;
        this.successMessage =
          'If an account exists with this email address, you will receive a password reset email shortly.';
      },
    });
  }
}
