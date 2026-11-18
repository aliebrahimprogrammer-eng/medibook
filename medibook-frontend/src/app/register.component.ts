
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, RegisterRequest } from './services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css',
})
export class RegisterComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  firstName = '';
  lastName = '';
  email = '';
  phone = '';
  password = '';
  confirmPassword = '';

  errorMessage = '';
  successMessage = '';
  isLoading = false;

  onRegister(): void {
    this.errorMessage = '';
    this.successMessage = '';

    const firstName = this.firstName.trim();
    const lastName = this.lastName.trim();
    const email = this.email.trim();

    if (!firstName || !lastName || !email || !this.password) {
      this.errorMessage = 'Please complete all required fields.';
      return;
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      this.errorMessage = 'Please enter a valid email address.';
      return;
    }

    if (this.password.length < 8 || this.password.length > 100) {
      this.errorMessage = 'Password must be between 8 and 100 characters.';
      return;
    }

    if (this.password !== this.confirmPassword) {
      this.errorMessage = 'Passwords do not match.';
      return;
    }

    const request: RegisterRequest = {
      firstName,
      lastName,
      email,
      password: this.password,
      phone: this.phone.trim() || undefined,
    };

    this.isLoading = true;

    this.authService.register(request).subscribe({
      next: () => {
        this.isLoading = false;
        this.successMessage =
          'Your account has been created! Please check your email and verify your account before signing in.';

        this.firstName = '';
        this.lastName = '';
        this.email = '';
        this.phone = '';
        this.password = '';
        this.confirmPassword = '';
      },
      error: (error) => {
        this.isLoading = false;

        if (error.status === 0) {
          this.errorMessage = 'Cannot reach the server. Please try again.';
        } else if (error.status === 409) {
          this.errorMessage = 'This email address is already registered.';
        } else {
          this.errorMessage =
            error.error?.message ||
            'Registration failed. Please check your information and try again.';
        }
      },
    });
  }
}
