# Forum App — Angular 22 + Tailwind + Signal Forms

Frontend del foro `forum.test` (Fase 2). Consume la API Spring Boot en `http://localhost:8081` con persistencia JSON y profundidad configurable.

## Stack

- **Angular 22.1.6** (standalone, zoneless, OnPush por defecto) + CLI 22.1.8 + TypeScript 6.0
- **Tailwind CSS 4.1.12** CSS-first (`@import 'tailwindcss'` + `@theme` en `src/styles.css`)
- **Vitest 4.1.11** + `jsdom` + `pnpm 12.4.1`
- **Signal Forms** (`@angular/forms/signals`) + `localStorage` + `HttpInterceptor`

## Requisitos

- Node `22.22.3` (vía `fnm`), `pnpm`, API en `:8081`

## Comandos

```bash
cd app
pnpm install                    # instala
pnpm approve-builds --all       # si pide aprobar esbuild/@parcel
pnpm run build                  # ng build -> dist/forum-app
pnpm exec ng test --watch=false # Vitest
pnpm start                      # ng serve -> http://localhost:4200 (proxy /api -> 8081)
```

## Estructura

```
src/app/
├── app.ts/html/css, app.config.ts (zoneless+HttpClient), app.routes.ts
├── core/ api.ts (13 endpoints), auth.ts (signals+normaliza usuario), token.interceptor.ts, auth.guard.ts, guest.guard.ts, notification.ts
├── shared/ app-header.ts/.html (#0a2240), relative-time.ts, depth.ts, notification-host.ts/.html
└── features/
    ├── auth/ login, register (Signal Forms, minúsculas sin espacios)
    ├── discussions/ discussion-list (tabs Todas/Mías/Participando), discussion-detail (árbol), discussion-create, discussions.ts
    ├── comments/ comment-tree (recursivo), comment-composer (Volver↔Comentar), comments.ts
    └── settings/ settings (cards 3/5/ilimitado, badge, hasChanges)
```

## Integración API (8081)

`proxy.conf.json` (`/api` → `http://localhost:8081`). 13 endpoints; usuario siempre en minúsculas sin espacios (`^[a-z0-9._-]+$`).

## Tema

`src/styles.css` `@theme` con `--color-ink #0a2240` (header), `--color-accent` dorado, `--radius-card`, `--shadow-card`. Notificaciones overlay centradas arriba (3 s).
