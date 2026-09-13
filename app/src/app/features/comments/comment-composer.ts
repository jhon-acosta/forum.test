import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-comment-composer',
  template: `<p>composer placeholder</p>`,
})
export class CommentComposer {
  @Input() discussionId = '';
}
