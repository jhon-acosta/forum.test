import { Component, Input } from '@angular/core';
import { CommentResponse } from '../../core/api';

@Component({
  selector: 'app-comment-tree',
  template: `<p>comment-tree placeholder</p>`,
})
export class CommentTree {
  @Input() comments: CommentResponse[] = [];
}
