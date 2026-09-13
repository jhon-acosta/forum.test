# Prompt 03 — Diseño de la App Angular

Actúa como Senior Angular Engineer. Diseña e implementa la Fase 2 del foro (frontend) sobre la API existente en `http://localhost:8081` (Fase 1).

## Contexto

- Backend (Fase 1) ya completo: Spring Boot 4.1.1 (Java 21), 12 endpoints bajo `/api`, auth con token opaco `Authorization: Bearer`, `maxReplyDepth` configurable.
- Frontend: Angular 22.1.x (standalone, zoneless, OnPush), Tailwind CSS 4, TypeScript 6, Vitest, pnpm.

## Requisitos funcionales

- **Autenticación:** registro, login (guarda `token` en `localStorage`), logout, guards que redirigen a `/auth/login?returnUrl=` si no hay token, y `guestGuard` que evita ver login autenticado.
- **Discusiones:** listar (`GET /api/discussions`), crear (`POST /api/discussions`), detalle con árbol (`GET /api/discussions/{id}`) y propias (`GET /api/users/me/discussions`).
- **Comentarios:** crear (`POST /api/discussions/{id}/comments` con `content` y `parentId`), árbol recursivo (`replies`), manejo de `422 Maximum reply depth exceeded`.
- **Settings:** `GET/PATCH /api/users/me/settings` con `maxReplyDepth` (3/5/null) y `null` como ilimitado.
- **Diseño:** profesional, no genérico, con Tailwind CSS-first y tokens propios en `@theme`.

## Restricciones

- Usar `@angular/forms/signals` (Signal Forms estable) para login/register/composer/create.
- `style=tailwind` (CSS-first), no SCSS; `file-name-style-guide=2025`; `test-runner=vitest`; `zoneless`.
- `localStorage` para la sesión; interceptor que añade `Bearer` y limpia en `401`.
- Sin `withFetch()` (deprecado).

## Entregables de este prompt

Diseño de la estructura `core/`, `features/`, `shared/`, rutas con `loadComponent`, y plan de 9 pasos con commits.
