import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormField, form, maxLength, required, submit } from '@angular/forms/signals';
import { CommentsService } from './comments';
import { NotificationService } from '../../core/notification';
import { ApiErrorResponse } from '../../core/api';

@Component({
  selector: 'app-comment-composer',
  imports: [FormField, RouterLink],
  templateUrl: './comment-composer.html',
})
export class CommentComposer {
  @Input({ required: true }) discussionId = '';
  @Input() parentId: string | null = null;
  @Input() placeholder = 'Escribe un comentario…';
  @Input() autofocus = false;
  @Input() showVolver = false;
  @Output() created = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  private readonly commentsService = inject(CommentsService);
  private readonly notification = inject(NotificationService);

  protected readonly model = signal({ content: '' });
  protected readonly composerForm = form(this.model, (p) => {
    required(p.content, { message: 'Contenido requerido' });
    maxLength(p.content, 5000, { message: 'Máximo 5000 caracteres' });
  });

  protected readonly submitting = signal(false);

  protected onSubmit(event: Event) {
    event.preventDefault();
    submit(this.composerForm, async () => {
      this.submitting.set(true);
      try {
        const { content } = this.model();
        await new Promise<void>((resolve, reject) => {
          this.commentsService.create(this.discussionId, content, this.parentId).subscribe({
            next: () => resolve(),
            error: (err) => reject(err),
          });
        });
        this.model.set({ content: '' });
        this.notification.success('Comentario creado');
        this.created.emit();
      } catch (err) {
        const apiErr = err as { status?: number; error?: ApiErrorResponse };
        const msg = apiErr?.error?.message ?? '';
        if (apiErr?.status === 422 || msg.includes('Maximum reply depth')) {
          this.notification.error('Máxima profundidad de respuestas alcanzada para esta discusión');
        } else if (apiErr?.status === 404) {
          this.notification.error('Discusión o comentario padre no encontrado');
        } else {
          this.notification.error(msg || 'Error al crear el comentario');
        }
      } finally {
        this.submitting.set(false);
      }
    });
  }

  protected onCancel() {
    this.model.set({ content: '' });
    this.cancelled.emit();
  }
}
