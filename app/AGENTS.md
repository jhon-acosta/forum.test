# AGENTS.md — app (Angular)

Guía para agentes de IA y desarrolladores que trabajen en el frontend.

## Stack (Fase 2) — backend ya disponible en :8081

- **Angular 18+** (standalone, TypeScript) + **Angular CLI** (`npx ng` o `npm i -g @angular/cli`).
- Estado de sesión con token opaco en `localStorage` + `HttpInterceptor` que añade `Authorization: Bearer <token>`.
- **Backend listo:** `http://localhost:8081` (ver `bruno/forum-api` y `marvel-test.sh`). 12 endpoints bajo `/api`; CORS permite `http://localhost:4200` → `8081`.

## Comandos

```bash
npx @angular/cli new app --routing --style=css   # scaffold (pendiente, repo ya tiene app/AGENTS.md)
cd app
npm install
npm start        # ng serve -> http://localhost:4200 (proxy a 8081)
npm test         # Karma/Jest
npm run build
```

## Integración con la API (precisa, 8081)

Base `http://localhost:8081`. Header `Authorization: Bearer {{token}}` tras `POST /api/auth/login` (`{{token}}` guardado por `bruno/02-login.bru`).

| Método | Ruta | Uso en app |
|--------|------|------------|
| POST | `/api/auth/register` | formulario registro |
| POST | `/api/auth/login` | guarda token |
| POST | `/api/auth/logout` | limpia token |
| GET | `/api/users/me` | perfil |
| GET/PATCH | `/api/users/me/settings` | `maxReplyDepth` (number\|null, absent=no cambia) |
| GET | `/api/discussions` | feed |
| POST | `/api/discussions` | crear discusión |
| GET | `/api/discussions/{id}` | detalle con `comments:[{replies:[]}]` |
| GET | `/api/users/me/discussions` | mis discusiones |
| POST | `/api/discussions/{id}/comments` | crear comentario/reply (`content`, `parentId?`) |
| GET | `/api/discussions/{id}/comments` | árbol alternativo |

Modelos: `User{id,username,maxReplyDepth,createdAt}`, `Discussion{id,title,content,author,createdAt}`, `Comment{id,parentId,content,author,createdAt,replies:[]}`. Errores en `{timestamp,status,error,message,path,details[]}` (ej. `422 Maximum reply depth exceeded`).

## Estructura prevista

```
app/src/app/
├── core/           # auth.service.ts, token.interceptor.ts, auth.guard.ts
├── features/
│   ├── auth/       # login.component, register.component
│   ├── discussions/# list.component, detail.component, create.component
│   └── comments/   # comment-tree.component (recursivo), composer.component
└── shared/         # pipes, ui
```

`comment-tree.component` renderiza `CommentResponse` recursivamente (como `DiscussionResponse.comments` del backend).

## Convenciones

- **Conventional Commits** con scopes `core|auth|discussions|comments`.
- No agregar comentarios al código salvo que se soliciten.
- Cada feature incluye tests.
- Commits y push requieren aprobación explícita del usuario.
- Documentar cambios de contrato (si el backend cambia `maxReplyDepth` o añade `GET /api/discussions?sort=...`).
