# AGENTS.md — app (Angular)

Guía para agentes de IA y desarrolladores que trabajen en el frontend.

## Stack (Fase 2 — backend en :8081)

- **Angular 22.1.6** (standalone, zoneless, OnPush por defecto) + **Angular CLI 22.1.8** + **TypeScript 6.0**.
- **Tailwind CSS 4.1.12** CSS-first (`@import 'tailwindcss'` + `@theme` en `src/styles.css`, `.postcssrc.json` con `@tailwindcss/postcss`).
- **Vitest 4.1.11** (runner por defecto) + `jsdom`.
- **pnpm 12.4.1** (`packageManager` `pnpm@12.4.1`, `pnpm-workspace.yaml` con `allowBuilds`).
- Estado de sesión con token opaco en `localStorage` (`forum_token`, `forum_user`) + `HttpInterceptor` (`Authorization: Bearer`).
- Usuario siempre en minúsculas sin espacios (sanitizado en registro/login y validado en API `^[a-z0-9._-]+$`).
- Notificaciones overlay centradas arriba (`NotificationService` + `NotificationHost`, 3 s, `success`/`error`).
- **Backend listo:** `http://localhost:8081` (ver `bruno/forum-api` y `marvel-test.sh`). 13 endpoints bajo `/api`; CORS permite `http://localhost:4200` → `8081`.

## Comandos

```bash
cd app
pnpm install                    # instala (usa pnpm, no npm)
pnpm approve-builds --all       # aprueba esbuild, @parcel/watcher, etc. si es necesario
pnpm run build                  # ng build -> dist/forum-app
pnpm exec ng test --watch=false # Vitest (TestBed zoneless)
pnpm start                      # ng serve -> http://localhost:4200 (proxy a 8081)
```

`ng serve` usa `proxy.conf.json` (`/api` → `http://localhost:8081`).

## Integración con la API (precisa, 8081)

Base `http://localhost:8081`. Header `Authorization: Bearer {{token}}` tras `POST /api/auth/login`.

| Método | Ruta | Uso en app |
|--------|------|------------|
| POST | `/api/auth/register` | formulario registro |
| POST | `/api/auth/login` | guarda token + user |
| POST | `/api/auth/logout` | limpia sesión |
| GET | `/api/users/me` | perfil |
| GET/PATCH | `/api/users/me/settings` | `maxReplyDepth` (number\|null, absent=no cambia) |
| GET | `/api/discussions` | feed |
| POST | `/api/discussions` | crear discusión |
| GET | `/api/discussions/{id}` | detalle con `maxReplyDepth` + `comments:[{replies:[]}]` |
| GET | `/api/users/me/discussions` | mis discusiones |
| GET | `/api/users/me/participating` | participando (comenté y no es mía) |
| POST | `/api/discussions/{id}/comments` | crear comentario/reply (`content`, `parentId?`) |
| GET | `/api/discussions/{id}/comments` | árbol alternativo |

Modelos: `UserResponse{id,username,maxReplyDepth,createdAt}`, `DiscussionResponse{id,title,content,author,maxReplyDepth,createdAt,comments}`, `DiscussionSummary`, `CommentResponse{id,parentId,content,author,createdAt,replies:[]}`. Errores en `{timestamp,status,error,message,path,details[]}` (ej. `422 Maximum reply depth exceeded`).

## Estructura (naming 2025 — conciso)

```
app/src/app/
├── app.ts / app.html / app.css / app.config.ts / app.routes.ts
├── core/
│   ├── api.ts
│   ├── auth.ts                 # AuthService (signals)
│   ├── token.interceptor.ts
│   ├── auth.guard.ts
│   └── guest.guard.ts
├── features/
│   ├── auth/         login.ts/.html, register.ts/.html
│   ├── discussions/  discussion-list.ts/.html, discussion-detail.ts/.html, discussion-create.ts/.html, discussions.ts (service)
│   ├── comments/     comment-tree.ts/.html, comment-composer.ts/.html, comments.ts (service)
│   └── settings/     settings.ts/.html
└── shared/
    └── relative-time.ts
```

`styles.css` con `@theme` (tokens `--color-canvas/surface/ink/muted/line/accent`, `--radius-card`, `--shadow-card`).

## Convenciones

- **Conventional Commits** con scopes `app|core|auth|discussions|comments|settings`.
- No agregar comentarios al código salvo que se soliciten.
- Cada feature incluye tests (Vitest + TestBed zoneless).
- Commits y push requieren aprobación explícita del usuario.
- Documentar cambios de contrato.
