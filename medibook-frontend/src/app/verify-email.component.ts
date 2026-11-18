
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './verify-email.component.html',
  styleUrl: './verify-email.component.css',
})
export class VerifyEmailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);

  status: 'loading' | 'success' | 'error' = 'loading';
  message = 'We are verifying your email address...';

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');

    if (!token) {
      this.status = 'error';
      this.message = 'Verification token is missing.';
      return;
    }

    this.authService.verifyEmail(token).subscribe({
      next: () => {
        this.status = 'success';
        this.message = 'Your email has been verified successfully!';
      },
      error: (error) => {
        this.status = 'error';

        if (error.status === 0) {
          this.message =
            'Unable to connect to the server. Please try again later.';
        } else {
          this.message =
            'This verification link is invalid, expired, or has already been used.';
        }
      },
    });
  }
}
