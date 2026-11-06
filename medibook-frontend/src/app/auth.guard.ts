
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';

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

export const roleGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const profileService = inject(ProfileService);
  const router = inject(Router);

  console.log('[Guard] Activated for:', route.routeConfig?.path);
  console.log('[Guard] Token exists:', !!authService.getToken());

  if (!authService.isAuthenticated()) {
    console.warn('[Guard] Redirecting: no token');
    return router.parseUrl('/login');
  }

  const allowedRoles = route.data['roles'] as UserRole[];

  return profileService.getMyProfile().pipe(
    map((profile) => {
      console.log('[Guard] Profile:', profile);
      console.log('[Guard] Allowed roles:', allowedRoles);

      if (allowedRoles.includes(profile.role)) {
        console.log('[Guard] Access granted');
        return true;
      }

      console.warn('[Guard] Wrong role:', profile.role);
      return router.parseUrl(dashboardPath(profile.role));
    }),
    catchError((error) => {
      console.error(
        '[Guard] Profile request failed:',
        error.status,
        error.message,
        error.error,
      );

      // Don't erase the token while diagnosing the failure.
      return of(router.parseUrl('/login'));
    }),
  );
};
