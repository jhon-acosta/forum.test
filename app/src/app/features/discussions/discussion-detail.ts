import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DiscussionResponse } from '../../core/api';
import { DiscussionsService } from './discussions';
import { RelativeTimePipe } from '../../shared/relative-time';
import { CommentTree } from '../comments/comment-tree';
import { CommentComposer } from '../comments/comment-composer';
import { AppHeader } from '../../shared/app-header';

@Component({
  selector: 'app-discussion-detail',
  imports: [RelativeTimePipe, CommentTree, CommentComposer, AppHeader],
  templateUrl: './discussion-detail.html',
})
export class DiscussionDetail {
  private readonly route = inject(ActivatedRoute);
  private readonly discussionsService = inject(DiscussionsService);

  protected readonly discussion = signal<DiscussionResponse | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  constructor() {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error.set('ID no válido');
      this.loading.set(false);
      return;
    }
    this.discussionsService.get(id).subscribe({
      next: (data) => {
        this.discussion.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar la discusión');
        this.loading.set(false);
      },
    });
  }

  protected onCommentCreated() {
    const d = this.discussion();
    if (!d) return;
    this.loading.set(true);
    this.discussionsService.get(d.id).subscribe({
      next: (data) => {
        this.discussion.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
