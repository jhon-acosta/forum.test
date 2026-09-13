import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { guestGuard } from './core/guest.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'discussions' },
  {
    path: 'auth/login',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login').then((m) => m.Login),
  },
  {
    path: 'auth/register',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/register').then((m) => m.Register),
  },
  {
    path: 'discussions',
    canActivate: [authGuard],
    loadComponent: () => import('./features/discussions/discussion-list').then((m) => m.DiscussionList),
  },
  {
    path: 'discussions/new',
    canActivate: [authGuard],
    loadComponent: () => import('./features/discussions/discussion-create').then((m) => m.DiscussionCreate),
  },
  {
    path: 'discussions/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./features/discussions/discussion-detail').then((m) => m.DiscussionDetail),
  },
  {
    path: 'settings',
    canActivate: [authGuard],
    loadComponent: () => import('./features/settings/settings').then((m) => m.Settings),
  },
  { path: '**', redirectTo: 'discussions' },
];
