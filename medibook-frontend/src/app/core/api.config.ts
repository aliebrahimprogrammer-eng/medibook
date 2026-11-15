import { environment } from '../../environments/environment';

export const API_BASE_URL = environment.apiBaseUrl;

export const API_ENDPOINTS = {
  auth: `${API_BASE_URL}/api/auth`,
  profile: `${API_BASE_URL}/api/profile`,
  adminUsers: `${API_BASE_URL}/api/admin/users`,
  appointments: `${API_BASE_URL}/api/appointments`,
  availabilities: `${API_BASE_URL}/api/availabilities`,
  doctors: `${API_BASE_URL}/api/doctors`,
  specializations: `${API_BASE_URL}/api/specializations`,
  waitlist: `${API_BASE_URL}/api/waitlist`,
  userAuditLogs: `${API_BASE_URL}/api/user-audit-logs`,
  businessAuditLogs: `${API_BASE_URL}/api/business-audit-logs`,
  notifications: `${API_BASE_URL}/api/notifications`,
  uploads: `${API_BASE_URL}/uploads`
} as const;