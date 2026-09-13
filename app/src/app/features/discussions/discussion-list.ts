import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DiscussionsService } from './discussions';
import { DiscussionSummary } from '../../core/api';
import { AuthService } from '../../core/auth';
import { RelativeTimePipe } from '../../shared/relative-time';

@Component({
  selector: 'app-discussion-list',
  imports: [RouterLink, RelativeTimePipe],
  templateUrl: './discussion-list.html',
})
export class DiscussionList {
  private readonly discussionsService = inject(DiscussionsService);
  protected readonly auth = inject(AuthService);

  protected readonly discussions = signal<DiscussionSummary[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  constructor() {
    this.load();
  }

  private load() {
    this.loading.set(true);
    this.error.set(null);
    this.discussionsService.list().subscribe({
      next: (data) => {
        this.discussions.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar las discusiones');
        this.loading.set(false);
      },
    });
  }

  protected logout() {
    this.auth.logout().subscribe();
  }
}
