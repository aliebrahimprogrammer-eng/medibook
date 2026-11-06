
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Doctor {
  id: number;
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  specializationId: number;
  specializationName: string;
  licenseNumber: string;
  bio: string;
}

export interface DoctorPage {
  content: Doctor[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class DoctorService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/doctors';

  searchDoctors(
    name: string,
    specialization: string,
    page = 0,
    size = 8,
  ): Observable<DoctorPage> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);

    if (name.trim()) {
      params = params.set('name', name.trim());
    }

    if (specialization.trim()) {
      params = params.set('specialization', specialization.trim());
    }

    return this.http.get<DoctorPage>(this.apiUrl, { params });
  }

  getDoctorById(id: number): Observable<Doctor> {
    return this.http.get<Doctor>(`${this.apiUrl}/${id}`);
  }

  getMyDoctorProfile(): Observable<Doctor> {
    return this.http.get<Doctor>(`${this.apiUrl}/me`);
  }
}
