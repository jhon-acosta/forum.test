import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';
import { API_BASE, AuthResponse, UserResponse } from './api';

const TOKEN_KEY = 'forum_token';
const USER_KEY = 'forum_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly tokenSignal = signal<string | null>(this.readToken());
  private readonly userSignal = signal<UserResponse | null>(this.readUser());

  readonly token = this.tokenSignal.asReadonly();
  readonly user = this.userSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.tokenSignal() !== null);

  register(username: string, password: string) {
    return this.http.post<UserResponse>(`${API_BASE}/auth/register`, { username, password });
  }

  login(username: string, password: string) {
    return this.http
      .post<AuthResponse>(`${API_BASE}/auth/login`, { username, password })
      .pipe(tap((res) => this.setSession(res.token, res.user)));
  }

  logout() {
    return this.http.post<void>(`${API_BASE}/auth/logout`, {}).pipe(
      tap({
        next: () => this.clearSession(),
        error: () => this.clearSession(),
      }),
    );
  }

  setSession(token: string, user: UserResponse) {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    this.tokenSignal.set(token);
    this.userSignal.set(user);
  }

  clearSession() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.tokenSignal.set(null);
    this.userSignal.set(null);
  }

  private readToken(): string | null {
    try {
      return localStorage.getItem(TOKEN_KEY);
    } catch {
      return null;
    }
  }

  private readUser(): UserResponse | null {
    try {
      const raw = localStorage.getItem(USER_KEY);
      return raw ? (JSON.parse(raw) as UserResponse) : null;
    } catch {
      return null;
    }
  }
}
