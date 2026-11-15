import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_ENDPOINTS } from '../core/api.config';

export interface Specialization {
  id: number;
  name: string;
  description: string;
}

@Injectable({
  providedIn: 'root',
})
export class SpecializationService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = API_ENDPOINTS.specializations;

  getAll(): Observable<Specialization[]> {
    return this.http.get<Specialization[]>(this.apiUrl);
  }
}
