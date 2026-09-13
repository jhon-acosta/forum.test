import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_BASE, DiscussionResponse, DiscussionSummary } from '../../core/api';

@Injectable({ providedIn: 'root' })
export class DiscussionsService {
  private readonly http = inject(HttpClient);

  list() {
    return this.http.get<DiscussionSummary[]>(`${API_BASE}/discussions`);
  }

  my() {
    return this.http.get<DiscussionSummary[]>(`${API_BASE}/users/me/discussions`);
  }

  get(id: string) {
    return this.http.get<DiscussionResponse>(`${API_BASE}/discussions/${id}`);
  }

  create(title: string, content: string) {
    return this.http.post<DiscussionResponse>(`${API_BASE}/discussions`, { title, content });
  }
}
