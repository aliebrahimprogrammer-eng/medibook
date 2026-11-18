import { environment } from '../../environments/environment';

export const API_ENDPOINTS = {
  auth: `${environment.userServiceUrl}/api/auth`,
  profile: `${environment.userServiceUrl}/api/profile`,
  adminUsers: `${environment.userServiceUrl}/api/admin/users`,
  userAuditLogs: `${environment.userServiceUrl}${environment.userAuditLogsPath}`,
  appointments: `${environment.businessServiceUrl}/api/appointments`,
  availabilities: `${environment.businessServiceUrl}/api/availabilities`,
  doctors: `${environment.businessServiceUrl}/api/doctors`,
  specializations: `${environment.businessServiceUrl}/api/specializations`,
  waitlist: `${environment.businessServiceUrl}/api/waitlist`,
  businessAuditLogs: `${environment.businessServiceUrl}${environment.businessAuditLogsPath}`,

  notifications: `${environment.notificationServiceUrl}/api/notifications`,

  uploads: `${environment.businessServiceUrl}/uploads`
} as const;
