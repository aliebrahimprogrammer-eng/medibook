
import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from './services/auth.service';
import {
  ProfileService,
  UserProfile,
} from './services/profile.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
})
export class DashboardComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly profileService = inject(ProfileService);
  private readonly router = inject(Router);

  profile = signal<UserProfile | null>(null);
  errorMessage = signal('');
  isLoading = signal(true);

  ngOnInit(): void {
    console.log('Dashboard initialized');

    this.profileService.getMyProfile().subscribe({
      next: (profile) => {
        console.log('Dashboard profile received:', profile);

        this.profile.set(profile);
        this.isLoading.set(false);

        console.log('Loading state:', this.isLoading());
      },
      error: (error) => {
        console.error('Dashboard profile error:', error);

        this.isLoading.set(false);
        this.errorMessage.set(
          'Could not load your profile. HTTP status: ' + error.status,
        );
      },
    });
  }

  logout(): void {
    this.authService.logout();
    void this.router.navigateByUrl('/login');
  }
}
