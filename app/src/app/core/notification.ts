import { Injectable, signal } from '@angular/core';

export type NotificationType = 'success' | 'error' | 'info' | 'warning';

export interface AppNotification {
  id: string;
  type: NotificationType;
  message: string;
  description?: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly _notifications = signal<AppNotification[]>([]);
  readonly notifications = this._notifications.asReadonly();

  private nextId = 0;

  private show(type: NotificationType, message: string, description?: string, duration = 3000) {
    const id = `n-${Date.now()}-${this.nextId++}`;
    const notification: AppNotification = { id, type, message, description };
    this._notifications.update((list) => [...list, notification]);
    if (duration > 0) {
      setTimeout(() => this.dismiss(id), duration);
    }
  }

  success(message: string, description?: string) {
    this.show('success', message, description);
  }

  error(message: string, description?: string) {
    this.show('error', message, description);
  }

  info(message: string, description?: string) {
    this.show('info', message, description);
  }

  warning(message: string, description?: string) {
    this.show('warning', message, description);
  }

  dismiss(id: string) {
    this._notifications.update((list) => list.filter((n) => n.id !== id));
  }

  clear() {
    this._notifications.set([]);
  }
}
