import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-comment-composer',
  template: `<p class="text-sm text-muted">composer placeholder (paso 6)</p>`,
})
export class CommentComposer {
  @Input() discussionId = '';
  @Input() parentId: string | null = null;
  @Output() created = new EventEmitter<void>();
}
