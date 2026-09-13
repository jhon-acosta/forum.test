import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormField, form, minLength, pattern, required, submit } from '@angular/forms/signals';
import { AuthService } from '../../core/auth';
import { NotificationService } from '../../core/notification';
import { ApiErrorResponse } from '../../core/api';

@Component({
  selector: 'app-login',
  imports: [FormField, RouterLink],
  templateUrl: './login.html',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly notification = inject(NotificationService);

  protected readonly model = signal({ username: '', password: '' });
  protected readonly loginForm = form(this.model, (p) => {
    required(p.username, { message: 'Usuario requerido' });
    minLength(p.username, 3, { message: 'Mínimo 3 caracteres' });
    pattern(p.username, /^[a-z0-9._-]+$/, { message: 'Solo minúsculas, números, . _ -' });
    required(p.password, { message: 'Contraseña requerida' });
    minLength(p.password, 6, { message: 'Mínimo 6 caracteres' });
  });

  protected readonly submitting = signal(false);

  protected onUsernameInput(event: Event) {
    const value = (event.target as HTMLInputElement).value.replace(/\s+/g, '').toLowerCase();
    this.model.update((m) => ({ ...m, username: value }));
  }

  protected onSubmit(event: Event) {
    event.preventDefault();
    submit(this.loginForm, async () => {
      this.submitting.set(true);
      try {
        const { username, password } = this.model();
        const cleanUsername = username.replace(/\s+/g, '').toLowerCase();
        await new Promise<void>((resolve, reject) => {
          this.auth.login(cleanUsername, password).subscribe({
            next: () => resolve(),
            error: (err) => reject(err),
          });
        });
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        const target = returnUrl && returnUrl.startsWith('/') && !returnUrl.startsWith('//') ? returnUrl : '/discussions';
        this.router.navigateByUrl(target);
      } catch (err) {
        const apiErr = err as { error?: ApiErrorResponse };
        const msg = apiErr?.error?.message ?? 'Error al iniciar sesión';
        this.notification.error(msg.includes('Invalid credentials') ? 'Credenciales inválidas' : msg);
      } finally {
        this.submitting.set(false);
      }
    });
  }
}
