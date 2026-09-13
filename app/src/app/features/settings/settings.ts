import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { API_BASE, SettingsResponse } from '../../core/api';
import { AuthService } from '../../core/auth';

@Component({
  selector: 'app-settings',
  imports: [RouterLink],
  templateUrl: './settings.html',
})
export class Settings {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  protected readonly maxReplyDepth = signal<number | null>(3);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly success = signal<string | null>(null);

  constructor() {
    this.load();
  }

  private load() {
    this.loading.set(true);
    this.error.set(null);
    this.http.get<SettingsResponse>(`${API_BASE}/users/me/settings`).subscribe({
      next: (res) => {
        this.maxReplyDepth.set(res.maxReplyDepth);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar la configuración');
        this.loading.set(false);
      },
    });
  }

  protected selectDepth(value: number | null) {
    this.maxReplyDepth.set(value);
    this.success.set(null);
  }

  protected save() {
    this.saving.set(true);
    this.error.set(null);
    this.success.set(null);
    const body = { maxReplyDepth: this.maxReplyDepth() };
    this.http.patch<SettingsResponse>(`${API_BASE}/users/me/settings`, body).subscribe({
      next: (res) => {
        this.maxReplyDepth.set(res.maxReplyDepth);
        const user = this.auth.user();
        if (user) {
          this.auth.setSession(this.auth.token()!, { ...user, maxReplyDepth: res.maxReplyDepth });
        }
        this.success.set('Configuración guardada');
        this.saving.set(false);
      },
      error: (err) => {
        const msg = (err as { error?: { message?: string } })?.error?.message ?? 'Error al guardar';
        this.error.set(msg);
        this.saving.set(false);
      },
    });
  }
}
