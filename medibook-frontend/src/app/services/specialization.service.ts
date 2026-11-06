import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

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

  private readonly apiUrl = 'http://localhost:8080/api/specializations';

  getAll(): Observable<Specialization[]> {
    return this.http.get<Specialization[]>(this.apiUrl);
  }
}
