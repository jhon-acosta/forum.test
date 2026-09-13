# Plan — Fase 2: App Angular — Estado final

> Refleja lo ya construido (Angular 22.1.6 + Tailwind 4 + Signal Forms + Vitest) y documenta cómo reproducirlo. No modifica la API.

## 1. Objetivo y arquitectura

App foro en monorepo `app/` que consume la API de Fase 1 en `http://localhost:8081` (proxy `/api`→8081 en `ng serve`). Diseño profesional con tokens propios, rutas protegidas y árbol de comentarios recursivo.

## 2. Decisiones fijadas

| Tema | Decisión final |
|---|---|
| Framework | Angular 22.1.6 (standalone, zoneless, OnPush por defecto), CLI 22.1.8, TypeScript 6.0 |
| Estilos | Tailwind CSS 4.1.12 CSS-first (`@import 'tailwindcss'` + `@theme` en `src/styles.css`), `.postcssrc.json` con `@tailwindcss/postcss` |
| Naming | `file-name-style-guide=2025` (`app.ts`, `auth.ts`) |
| Formularios | Signal Forms (`@angular/forms/signals`: `form`, `FormField`, `required`, `minLength`, `pattern`, `submit`) |
| Estado | `localStorage` (`forum_token`, `forum_user`) + `signal`/`computed` + `httpResource`/`HttpClient` |
| Router | Guards funcionales `authGuard`/`guestGuard`, `returnUrl` saneada (`/…` y no `//`) |
| Http | `provideHttpClient(withInterceptors([tokenInterceptor]))` (sin `withFetch()` deprecado) |
| Tests | Vitest 4.1.11 + `jsdom`, `TestBed` zoneless |
| Package | `pnpm 12.4.1` (`pnpm-workspace.yaml` con `allowBuilds` para `esbuild`, `@parcel/watcher`) |
| Palette | `#0a2240` header (`--color-ink`), acento dorado, texto `ink` sobre accent; `Inter` + `JetBrains Mono` |

## 3. Estructura

```
app/
├── angular.json, package.json, .postcssrc.json, proxy.conf.json, tsconfig*.json
├── public/favicon.ico
└── src/
    ├── styles.css (@import "tailwindcss"; @theme con --color-ink #0a2240, --color-accent, --radius-card, --shadow-card)
    ├── main.ts, index.html
    └── app/
        ├── app.ts/html/css, app.config.ts (zoneless + HttpClient + Router), app.routes.ts
        ├── core/ api.ts (13 endpoints), auth.ts (signals + normaliza usuario), token.interceptor.ts, auth.guard.ts, guest.guard.ts, notification.ts
        ├── shared/ app-header.ts/.html (Discusiones + dropdown Configuración/Cerrar sesión), relative-time.ts, depth.ts, notification-host.ts/.html, toast-container
        └── features/
            ├── auth/ login.ts/.html, register.ts/.html (Signal Forms, pattern minúsculas, notificación overlay)
            ├── discussions/ discussion-list.ts/.html (tabs Todas/Mías/Participando con conteos), discussion-detail.ts/.html (árbol + composer con showVolver), discussion-create.ts/.html (Cancelar ↔ Crear), discussions.ts (list/my/participating/get/create)
            ├── comments/ comment-tree.ts/.html (recursivo, canReplyAt), comment-composer.ts/.html (showVolver), comments.ts
            └── settings/ settings.ts/.html (cards 3/5/Ilimitado, badge, hasChanges, notificación)
```

## 4. Contrato de API consumido (13 endpoints)

| Método | Ruta | Uso |
|---|---|---|
| POST | `/api/auth/register` | registro (username normalizado) |
| POST | `/api/auth/login` | guarda `token` |
| POST | `/api/auth/logout` | limpia |
| GET | `/api/users/me` | perfil |
| GET/PATCH | `/api/users/me/settings` | `maxReplyDepth` |
| GET | `/api/discussions` | Todas |
| GET | `/api/users/me/discussions` | Mías |
| GET | `/api/users/me/participating` | Participando (comenté y no es mía) |
| POST | `/api/discussions` | crear |
| GET | `/api/discussions/{id}` | detalle `{maxReplyDepth, comments}` |
| POST | `/api/discussions/{id}/comments` | crear (`parentId?`) → 422 si límite |
| GET | `/api/discussions/{id}/comments` | árbol |

## 5. Rutas y guards

```ts
'' -> redirectTo 'discussions'
'auth/login', 'auth/register' canActivate [guestGuard] -> redirectTo 'discussions' si autenticado
'discussions', 'discussions/new', 'discussions/:id', 'settings' canActivate [authGuard] -> redirectTo '/auth/login?returnUrl=...'
'**' -> 'discussions'
```

## 6. UI y UX profesional

- **Header** `Discusiones` en `bg-ink #0a2240 text-white` con dropdown `Configuración`/`Cerrar sesión` (texto `ink` sobre `surface`).
- **Tabs** `Todas (n)`/`Mías (n)`/`Participando (n)` con conteos y activo `bg-accent text-ink`.
- **Settings**: cards con descripción, badge `Actual: Ilimitado` (sin `null` crudo), `Guardar cambios` deshabilitado sin cambios.
- **Navegación**: `← Volver` opuesto a `Comentar` debajo del input; `Cancelar` opuesto a `Crear` (patrón primario derecha).
- **Notificaciones**: overlay centrado arriba (`NotificationHost` en `app.html`, 3s, `success`/`error`).
- **Username**: siempre `toLowerCase` + sin espacios (front sanitiza, API valida con `^[a-z0-9._-]+$`).

## 7. Persistencia y validación de usuario

- Registro/login normalizan `username` (`replace \s + toLowerCase`); unicidad case-insensitive; `409 El nombre de usuario ya existe` como toast.

## 8. Pruebas App — 8 (Vitest)

- `app.spec.ts` (router-outlet), `depth.spec.ts` (canReplyAt/canComment), + `NotificationService`/`notification-host` y `AppHeader`.

## 9. Pasos y commits ejecutados (resumen)

1. `chore(app): scaffold Angular 22 with Tailwind, routing and Vitest`
2. `chore(app): configure Tailwind design tokens and dev proxy`
3. `feat(app): add auth core, guards and token interceptor`
4. `feat(app): add auth pages with signal forms`
5. `feat(app): add discussion features`
6. `feat(app): add nested comment tree and composer`
7. `feat(app): add reply depth settings`
8. `style(app): apply custom forum theme and layout`
9. `style(app): set header to #0a2240 and fix user dropdown`
10. `fix(app): make header dropdown items visible`
11. `feat: normalize usernames and add centered overlay alerts` (actual)

## 10. Prácticas aplicadas

- Tailwind CSS-first con `@theme`, sin SCSS; `withFetch` no usado; `localStorage` + interceptor + guards funcionales; Signal Forms con `pattern` y sanitización; `hasChanges` en settings; árbol recursivo con `canReplyAt`; `proxy.conf.json`; `pnpm` con `allowBuilds`; `OnPush` por defecto.
