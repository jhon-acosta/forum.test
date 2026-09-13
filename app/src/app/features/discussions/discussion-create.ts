import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormField, form, maxLength, required, submit } from '@angular/forms/signals';
import { DiscussionsService } from './discussions';

@Component({
  selector: 'app-discussion-create',
  imports: [FormField, RouterLink],
  templateUrl: './discussion-create.html',
})
export class DiscussionCreate {
  private readonly discussionsService = inject(DiscussionsService);
  private readonly router = inject(Router);

  protected readonly model = signal({ title: '', content: '' });
  protected readonly discussionForm = form(this.model, (p) => {
    required(p.title, { message: 'Título requerido' });
    maxLength(p.title, 150, { message: 'Máximo 150 caracteres' });
    required(p.content, { message: 'Contenido requerido' });
    maxLength(p.content, 10000, { message: 'Máximo 10000 caracteres' });
  });

  protected readonly serverError = signal<string | null>(null);
  protected readonly submitting = signal(false);

  protected onSubmit(event: Event) {
    event.preventDefault();
    this.serverError.set(null);
    submit(this.discussionForm, async () => {
      this.submitting.set(true);
      try {
        const { title, content } = this.model();
        const res = await new Promise<import('../../core/api').DiscussionResponse>((resolve, reject) => {
          this.discussionsService.create(title, content).subscribe({
            next: (data) => resolve(data),
            error: (err) => reject(err),
          });
        });
        this.router.navigate(['/discussions', res.id]);
      } catch (err) {
        const apiErr = err as { error?: import('../../core/api').ApiErrorResponse };
        this.serverError.set(apiErr?.error?.message ?? 'Error al crear la discusión');
      } finally {
        this.submitting.set(false);
      }
    });
  }
}
