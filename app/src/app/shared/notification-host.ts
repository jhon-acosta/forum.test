import { Component, inject } from '@angular/core';
import { NotificationService } from '../core/notification';

@Component({
  selector: 'app-notification-host',
  templateUrl: './notification-host.html',
})
export class NotificationHost {
  protected readonly notificationService = inject(NotificationService);
}
