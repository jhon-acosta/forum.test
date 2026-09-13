import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormField, form, minLength, pattern, required, submit } from '@angular/forms/signals';
import { AuthService } from '../../core/auth';
import { NotificationService } from '../../core/notification';
import { ApiErrorResponse } from '../../core/api';

@Component({
  selector: 'app-register',
  imports: [FormField, RouterLink],
  templateUrl: './register.html',
})
export class Register {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notification = inject(NotificationService);

  protected readonly model = signal({ username: '', password: '' });
  protected readonly registerForm = form(this.model, (p) => {
    required(p.username, { message: 'Usuario requerido' });
    minLength(p.username, 3, { message: 'Mínimo 3 caracteres' });
    pattern(p.username, /^[a-z0-9._-]+$/, { message: 'Solo minúsculas, números, punto, guion y guion bajo' });
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
    submit(this.registerForm, async () => {
      this.submitting.set(true);
      try {
        const { username, password } = this.model();
        const cleanUsername = username.replace(/\s+/g, '').toLowerCase();
        await new Promise<void>((resolve, reject) => {
          this.auth.register(cleanUsername, password).subscribe({
            next: () => resolve(),
            error: (err) => reject(err),
          });
        });
        this.notification.success('Cuenta creada', 'Ahora puedes iniciar sesión');
        this.router.navigate(['/auth/login']);
      } catch (err) {
        const apiErr = err as { error?: ApiErrorResponse; status?: number };
        const msg = apiErr?.error?.message ?? 'Error al registrarse';
        if (apiErr?.status === 409 || msg.includes('already exists')) {
          this.notification.error('El nombre de usuario ya existe');
        } else {
          this.notification.error(msg);
        }
      } finally {
        this.submitting.set(false);
      }
    });
  }
}
