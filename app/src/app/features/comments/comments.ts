import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_BASE, CommentResponse } from '../../core/api';

@Injectable({ providedIn: 'root' })
export class CommentsService {
  private readonly http = inject(HttpClient);

  create(discussionId: string, content: string, parentId: string | null) {
    const body: { content: string; parentId?: string | null } = { content };
    if (parentId) body.parentId = parentId;
    return this.http.post<CommentResponse>(`${API_BASE}/discussions/${discussionId}/comments`, body);
  }

  list(discussionId: string) {
    return this.http.get<CommentResponse[]>(`${API_BASE}/discussions/${discussionId}/comments`);
  }
}
