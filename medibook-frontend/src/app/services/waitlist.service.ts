import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type WaitlistStatus =
  | 'WAITING'
  | 'CANCELLED'
  | 'MATCHED';

export interface WaitlistRequest {
  doctorId: number;
  preferredStartDateTime: string;
  preferredEndDateTime: string;
}

export interface WaitlistEntry {
  id: number;
  patientId: number;
  doctorId: number;
  doctorName: string;
  preferredStartDateTime: string;
  preferredEndDateTime: string;
  status: WaitlistStatus;
  matchedAppointmentId: number | null;
  createdAt: string;
  updatedAt: string;
}

@Injectable({
  providedIn: 'root',
})
export class WaitlistService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = 'http://localhost:8080/api/waitlist';

  joinWaitlist(
    request: WaitlistRequest
  ): Observable<WaitlistEntry> {
    return this.http.post<WaitlistEntry>(
      this.apiUrl,
      request
    );
  }

  getMyWaitlist(): Observable<WaitlistEntry[]> {
    return this.http.get<WaitlistEntry[]>(
      `${this.apiUrl}/me`
    );
  }

  cancelWaitlistEntry(id: number): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/${id}`
    );
  }
}