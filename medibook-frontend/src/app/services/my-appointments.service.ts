
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PatientAppointment {
  id: number;
  patientId: number;
  doctorId: number;
  doctorName: string;
  startDateTime: string;
  endDateTime: string;
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED';
  reason: string;
  createdAt: string;
  updatedAt: string;
}

export interface AppointmentPage {
  content: PatientAppointment[];
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
export class MyAppointmentsService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = 'http://localhost:8080/api/appointments';

  getMyAppointments(
    page = 0,
    size = 10,
  ): Observable<AppointmentPage> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size);

    return this.http.get<AppointmentPage>(
      `${this.apiUrl}/my`,
      { params },
    );
  }

  cancelAppointment(id: number): Observable<void> {
    return this.http.patch<void>(
      `${this.apiUrl}/${id}/cancel`,
      {},
    );
  }
}
