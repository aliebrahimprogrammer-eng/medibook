
import { Routes } from '@angular/router';

import { LoginComponent } from './login.component';
import { DashboardComponent } from './dashboard.component';
import { DoctorsComponent } from './doctors.component';
import { BookingComponent } from './booking.component';
import { MyAppointmentsComponent } from './my-appointments.component';
import { MyWaitlistComponent } from './my-waitlist.component';
import { DoctorAppointmentsComponent } from './doctor-appointments.component';
import { DoctorAvailabilityComponent } from './doctor-availability.component';
import { AdminUsersComponent } from './admin-users.component';
import { AdminAuditLogsComponent } from './admin-audit-logs.component';
import { AdminCreateDoctorComponent } from './admin-create-doctor.component';
import { MyProfileComponent } from './my-profile.component';
import { roleGuard } from './auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    component: LoginComponent,
  },
  {
    path: 'profile',
    component: MyProfileComponent,
    canActivate: [roleGuard],
    data: { roles: ['PATIENT', 'DOCTOR', 'ADMIN'] },
  },
  {
    path: 'patient/doctors',
    component: DoctorsComponent,
    canActivate: [roleGuard],
    data: { roles: ['PATIENT'] },
  },
  {
    path: 'patient/book/:doctorId',
    component: BookingComponent,
    canActivate: [roleGuard],
    data: { roles: ['PATIENT'] },
  },
  {
    path: 'patient/appointments',
    component: MyAppointmentsComponent,
    canActivate: [roleGuard],
    data: { roles: ['PATIENT'] },
  },
  {
    path: 'patient/waitlist',
    component: MyWaitlistComponent,
    canActivate: [roleGuard],
    data: { roles: ['PATIENT'] },
  },
  {
    path: 'patient',
    component: DashboardComponent,
    canActivate: [roleGuard],
    data: { roles: ['PATIENT'] },
  },
  {
    path: 'doctor/appointments',
    component: DoctorAppointmentsComponent,
    canActivate: [roleGuard],
    data: { roles: ['DOCTOR'] },
  },
  {
    path: 'doctor/availability',
    component: DoctorAvailabilityComponent,
    canActivate: [roleGuard],
    data: { roles: ['DOCTOR'] },
  },
  {
    path: 'doctor',
    component: DashboardComponent,
    canActivate: [roleGuard],
    data: { roles: ['DOCTOR'] },
  },
  {
    path: 'admin/users',
    component: AdminUsersComponent,
    canActivate: [roleGuard],
    data: { roles: ['ADMIN'] },
  },
  {
    path: 'admin/audit-logs',
    component: AdminAuditLogsComponent,
    canActivate: [roleGuard],
    data: { roles: ['ADMIN'] },
  },
  {
    path: 'admin/create-doctor',
    component: AdminCreateDoctorComponent,
    canActivate: [roleGuard],
    data: { roles: ['ADMIN'] },
  },
  {
    path: 'admin',
    component: DashboardComponent,
    canActivate: [roleGuard],
    data: { roles: ['ADMIN'] },
  },
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full',
  },
  {
    path: '**',
    redirectTo: 'login',
  },
];
