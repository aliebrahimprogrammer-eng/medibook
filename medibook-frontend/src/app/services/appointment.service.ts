
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AppointmentRequest {
  doctorId: number;
  startDateTime: string;
  endDateTime: string;
  reason?: string;
}


export interface AppointmentResponse {
  id: number;
  patientId: number;
  doctorId: number;
  doctorName: string;
  patientName: string;
  startDateTime: string;
  endDateTime: string;
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED';
  reason: string;
}

export interface AppointmentPage {
  content: AppointmentResponse[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class AppointmentService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/appointments';

  createAppointment(
    request: AppointmentRequest,
  ): Observable<AppointmentResponse> {
    return this.http.post<AppointmentResponse>(this.apiUrl, request);
  }

  getMyDoctorAppointments(
    page = 0,
    size = 10,
  ): Observable<AppointmentPage> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size);

    return this.http.get<AppointmentPage>(
      `${this.apiUrl}/doctor/my`,
      { params },
    );
  }


updateAppointmentStatus(
  id: number,
  status: AppointmentResponse['status'],
): Observable<void> {
  return this.http.patch<void>(
    `${this.apiUrl}/${id}/status`,
    { status },
  );
}

}
