
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.css',
})
export class ResetPasswordComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);

  token = '';
  newPassword = '';
  confirmPassword = '';

  status: 'form' | 'success' | 'error' = 'form';
  errorMessage = '';
  isLoading = false;

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';

    if (!this.token) {
      this.status = 'error';
      this.errorMessage = 'The password reset token is missing.';
    }
  }

  onSubmit(): void {
    this.errorMessage = '';

    if (!this.newPassword || this.newPassword.length < 8 ||
      this.newPassword.length > 100) {
      this.errorMessage =
        'Password must be between 8 and 100 characters.';
      return;
    }

    if (this.newPassword !== this.confirmPassword) {
      this.errorMessage = 'Passwords do not match.';
      return;
    }

    this.isLoading = true;

    this.authService.resetPassword({
      token: this.token,
      newPassword: this.newPassword,
    }).subscribe({
      next: () => {
        this.isLoading = false;
        this.status = 'success';
        this.newPassword = '';
        this.confirmPassword = '';
      },
      error: (error) => {
        this.isLoading = false;

        if (error.status === 0) {
          this.errorMessage =
            'Unable to connect to the server. Please try again.';
        } else {
          this.errorMessage =
            error.error?.message ||
            'This reset link is invalid or has expired. Please request a new one.';
        }
      },
    });
  }
}
