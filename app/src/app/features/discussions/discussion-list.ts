import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DiscussionsService } from './discussions';
import { DiscussionSummary } from '../../core/api';
import { RelativeTimePipe } from '../../shared/relative-time';
import { AppHeader } from '../../shared/app-header';
import { NotificationService } from '../../core/notification';

@Component({
  selector: 'app-discussion-list',
  imports: [RouterLink, RelativeTimePipe, AppHeader],
  templateUrl: './discussion-list.html',
})
export class DiscussionList {
  private readonly discussionsService = inject(DiscussionsService);
  private readonly notification = inject(NotificationService);

  protected readonly activeTab = signal<'all' | 'mine' | 'participating'>('all');

  protected readonly all = signal<DiscussionSummary[]>([]);
  protected readonly mine = signal<DiscussionSummary[]>([]);
  protected readonly participating = signal<DiscussionSummary[]>([]);

  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly visible = computed(() => {
    switch (this.activeTab()) {
      case 'mine':
        return this.mine();
      case 'participating':
        return this.participating();
      default:
        return this.all();
    }
  });

  constructor() {
    this.loadAll();
  }

  private loadAll() {
    this.loading.set(true);
    this.error.set(null);
    let pending = 3;
    const done = () => {
      pending--;
      if (pending === 0) this.loading.set(false);
    };

    this.discussionsService.list().subscribe({
      next: (data) => {
        this.all.set(data);
        done();
      },
      error: () => {
        this.notification.error('No se pudieron cargar las discusiones');
        done();
      },
    });

    this.discussionsService.my().subscribe({
      next: (data) => {
        this.mine.set(data);
        done();
      },
      error: () => done(),
    });

    this.discussionsService.participating().subscribe({
      next: (data) => {
        this.participating.set(data);
        done();
      },
      error: () => done(),
    });
  }

  protected setTab(tab: 'all' | 'mine' | 'participating') {
    this.activeTab.set(tab);
  }
}
