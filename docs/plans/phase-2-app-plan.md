# Plan Fase 2 — App Angular (para otro agente)

> Documento autocontenido para ejecutar la Fase 2 sin contexto previo. `app/` solo contenía `AGENTS.md`; la API (Fase 1) está en `http://localhost:8081`.

## 0. Reglas de trabajo

1. Trabajo por pasos (sección 6). Al terminar cada paso: verificación y detenerse.
2. Cada commit requiere aprobación explícita.
3. `git push` solo al final con confirmación.
4. Conventional Commits (`feat|fix|test|docs|chore|style|refactor`).
5. No añadir comentarios al código.
6. Cada feature incluye sus pruebas (Vitest).
7. No tocar `api/` salvo que el paso lo indique.

## 1. Contexto

```
forum.test/
├── README.md
├── .gitignore
├── api/ (Spring Boot 4.1.1, NO TOCAR)
├── app/ (AGENTS.md)
├── bruno/forum-api/ (host 8081)
├── marvel-test.sh (tony/natasha/bruce secret123)
└── docs/{prompts,plans}
```

Entorno: Node 22.22.3, pnpm 12.4.1, Angular CLI 22.1.8, Tailwind 4.3.3, API :8081.

## 2. Decisiones fijadas

| Tema | Decisión |
|---|---|
| Framework | Angular 22.1.x (standalone, zoneless, OnPush) |
| Estilos | Tailwind 4 CSS-first (`--style=tailwind`, sin SCSS) |
| Naming | 2025 (`app.ts`, `auth.ts`) |
| Formularios | Signal Forms (`@angular/forms/signals`) |
| Sesión | `localStorage` (`forum_token`, `forum_user`) |
| Tests | Vitest |
| SSR | deshabilitado |
| Router | Guards funcionales + redirect |
| Http | `provideHttpClient(withInterceptors([tokenInterceptor]))` (sin `withFetch()`) |

## 3. Contrato de API (base `http://localhost:8081`)

| Método | Ruta | Auth | Request | Response | Errores |
|---|---|---|---|---|---|
| POST | `/api/auth/register` | no | `{username,password}` | 201 `UserResponse` | 400, 409 |
| POST | `/api/auth/login` | no | `{username,password}` | 200 `AuthResponse` | 401 |
| POST | `/api/auth/logout` | sí | — | 204 | 401 |
| GET | `/api/users/me` | sí | — | 200 `UserResponse` | 401 |
| GET/PATCH | `/api/users/me/settings` | sí | `{maxReplyDepth: number\|null}` | 200 `SettingsResponse` | 400, 401 |
| GET | `/api/discussions` | sí | — | 200 `DiscussionSummary[]` | 401 |
| POST | `/api/discussions` | sí | `{title,content}` | 201 `DiscussionResponse` | 400, 401 |
| GET | `/api/discussions/{id}` | sí | — | 200 `DiscussionResponse` (árbol) | 400, 401, 404 |
| GET | `/api/users/me/discussions` | sí | — | 200 `DiscussionSummary[]` | 401 |
| POST | `/api/discussions/{id}/comments` | sí | `{content,parentId?}` | 201 `CommentResponse` | 400, 401, 404, 422 |
| GET | `/api/discussions/{id}/comments` | sí | — | 200 `CommentResponse[]` | 401, 404 |

Modelos TS en `core/api.ts` (ver sección 5 del plan original).

## 4. Estructura objetivo

```
app/
├── angular.json, package.json, .postcssrc.json, proxy.conf.json
├── src/styles.css (@import "tailwindcss"; @theme)
└── src/app/
    ├── app.ts/html/css, app.config.ts, app.routes.ts
    ├── core/ api.ts, auth.ts, token.interceptor.ts, auth.guard.ts, guest.guard.ts
    ├── features/auth/ login.ts/.html, register.ts/.html
    ├── features/discussions/ discussion-list.ts/.html, discussion-detail.ts/.html, discussion-create.ts/.html, discussions.ts
    ├── features/comments/ comment-tree.ts/.html, comment-composer.ts/.html, comments.ts
    ├── features/settings/ settings.ts/.html
    └── shared/ relative-time.ts
```

## 5. Pasos

### Paso 1 — Scaffold

```bash
mkdir -p /tmp/opencode/forum-scaffold && cd /tmp/opencode/forum-scaffold
NG_CLI_ANALYTICS=false npx @angular/cli@22.1.8 new forum-app \
  --style=tailwind --routing --ssr=false --zoneless \
  --test-runner=vitest --package-manager=npm --skip-git \
  --file-name-style-guide=2025 --ai-config=none --skip-install
cp -a forum-app/. /path/app/  # preservar AGENTS.md
cd /path/app && pnpm install # requiere Node >=22.22.3 (usar fnm)
```

Commit: `chore(app): scaffold Angular 22 with Tailwind, routing and Vitest`

### Paso 2 — Tema + proxy

`src/styles.css` con `@theme` (tokens --color-canvas/surface/ink/muted/line/accent, --radius-card, --shadow-card) y `proxy.conf.json` (`/api` → 8081) + `angular.json` `proxyConfig` y `packageManager: pnpm`.

Commit: `chore(app): configure Tailwind design tokens and dev proxy`

### Paso 3 — Core

`core/api.ts`, `core/auth.ts` (signals + localStorage), `core/token.interceptor.ts`, `core/auth.guard.ts`, `core/guest.guard.ts`, `app.config.ts` (`provideHttpClient(withInterceptors)`), `app.routes.ts` (lazy `loadComponent` con guards).

Commit: `feat(app): add auth core, guards and token interceptor`

### Paso 4 — Auth UI

Signal Forms (`form`, `FormField`, `required`, `minLength`, `submit`) para `login`/`register` con manejo 401/409.

Commit: `feat(app): add auth pages with signal forms`

### Paso 5 — Discusiones

`discussions.ts` + `relative-time.ts` + `discussion-list/create/detail` con `HttpClient` y `signal`.

Commit: `feat(app): add discussion features`

### Paso 6 — Comentarios

`comments.ts` + `comment-tree` recursivo + `comment-composer` (422).

Commit: `feat(app): add nested comment tree and composer`

### Paso 7 — Settings

`settings.ts` con `GET/PATCH /api/users/me/settings` (3/5/null) y `AuthService` update.

Commit: `feat(app): add reply depth settings`

### Paso 8 — Tema

Pulir `styles.css` (`color-scheme`, `::selection`, `focus-visible`) y `app.html` (solo `<router-outlet />`).

Commit: `style(app): apply custom forum theme and layout`

### Paso 9 — Verificación

`pnpm run build` y `npx ng test --watch=false` en verde; actualizar `app/AGENTS.md` y `README.md`.

Commit: `docs(app): document phase 2, setup and tests`
