
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Availability {
  id: number;
  doctorId: number;
  startDateTime: string;
  endDateTime: string;
}

export interface AvailabilityPage {
  content: Availability[];
  totalPages: number;
  totalElements: number;
  number: number;
  last: boolean;
  empty: boolean;
}

export interface AvailabilityRequest {
  doctorId: number;
  startDateTime: string;
  endDateTime: string;
}

@Injectable({
  providedIn: 'root',
})
export class AvailabilityService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl =
    'http://localhost:8080/api/availabilities';

  getDoctorAvailability(
    doctorId: number,
    page = 0,
    size = 50,
  ): Observable<AvailabilityPage> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', 'startDateTime,asc');

    return this.http.get<AvailabilityPage>(
      `${this.apiUrl}/doctor/${doctorId}`,
      { params },
    );
  }

  createAvailability(
    request: AvailabilityRequest,
  ): Observable<Availability> {
    return this.http.post<Availability>(
      this.apiUrl,
      request,
    );
  }

  deleteAvailability(id: number): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/${id}`,
    );
  }
}
