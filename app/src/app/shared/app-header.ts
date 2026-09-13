import { UpperCasePipe } from '@angular/common';
import { Component, ElementRef, HostListener, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../core/auth';

@Component({
  selector: 'app-header',
  imports: [RouterLink, UpperCasePipe],
  templateUrl: './app-header.html',
})
export class AppHeader {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly menuOpen = signal(false);
  private readonly elementRef = inject(ElementRef);

  protected toggleMenu() {
    this.menuOpen.update((v) => !v);
  }

  protected closeMenu() {
    this.menuOpen.set(false);
  }

  @HostListener('document:click', ['$event'])
  protected onDocumentClick(event: MouseEvent) {
    if (!this.elementRef.nativeElement.contains(event.target as Node)) {
      this.closeMenu();
    }
  }

  @HostListener('document:keydown.escape')
  protected onEscape() {
    this.closeMenu();
  }

  protected logout() {
    this.closeMenu();
    this.auth.logout().subscribe({
      next: () => this.router.navigate(['/auth/login']),
      error: () => {
        this.auth.clearSession();
        this.router.navigate(['/auth/login']);
      },
    });
  }
}
