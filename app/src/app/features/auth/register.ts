import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormField, form, minLength, required, submit } from '@angular/forms/signals';
import { AuthService } from '../../core/auth';
import { ApiErrorResponse } from '../../core/api';

@Component({
  selector: 'app-register',
  imports: [FormField, RouterLink],
  templateUrl: './register.html',
})
export class Register {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly model = signal({ username: '', password: '' });
  protected readonly registerForm = form(this.model, (p) => {
    required(p.username, { message: 'Usuario requerido' });
    minLength(p.username, 3, { message: 'Mínimo 3 caracteres' });
    required(p.password, { message: 'Contraseña requerida' });
    minLength(p.password, 6, { message: 'Mínimo 6 caracteres' });
  });

  protected readonly serverError = signal<string | null>(null);
  protected readonly submitting = signal(false);

  protected onSubmit(event: Event) {
    event.preventDefault();
    this.serverError.set(null);
    submit(this.registerForm, async () => {
      this.submitting.set(true);
      try {
        const { username, password } = this.model();
        await new Promise<void>((resolve, reject) => {
          this.auth.register(username, password).subscribe({
            next: () => resolve(),
            error: (err) => reject(err),
          });
        });
        this.router.navigate(['/auth/login']);
      } catch (err) {
        const apiErr = err as { error?: ApiErrorResponse; status?: number };
        const msg = apiErr?.error?.message ?? 'Error al registrarse';
        if (apiErr?.status === 409 || msg.includes('already exists')) {
          this.serverError.set('El nombre de usuario ya existe');
        } else {
          this.serverError.set(msg);
        }
      } finally {
        this.submitting.set(false);
      }
    });
  }
}
