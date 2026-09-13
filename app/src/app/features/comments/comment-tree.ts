import { Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { CommentResponse } from '../../core/api';
import { RelativeTimePipe } from '../../shared/relative-time';
import { CommentComposer } from './comment-composer';

@Component({
  selector: 'app-comment-tree',
  imports: [RelativeTimePipe, CommentComposer],
  templateUrl: './comment-tree.html',
})
export class CommentTree {
  @Input({ required: true }) comments: CommentResponse[] = [];
  @Input({ required: true }) discussionId = '';
  @Input() level = 0;
  @Output() created = new EventEmitter<void>();

  protected readonly replyingTo = signal<string | null>(null);

  protected toggleReply(commentId: string) {
    this.replyingTo.update((current) => (current === commentId ? null : commentId));
  }

  protected onCreated() {
    this.replyingTo.set(null);
    this.created.emit();
  }
}
