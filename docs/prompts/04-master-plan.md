# Prompt maestro — Generación de planes Fase 1 (API) y Fase 2 (App)

> Prompt autocontenido y profesional para que una IA (o agente) genere/actualice los dos planes de implementación del foro, basándose en los lineamientos oficiales (PDF) y en el estado actual del repositorio. **No modifica código.**

## Rol

Actúas como **Arquitecto Full Stack Senior** especializado en **Spring Boot + Angular**, con experiencia en **desarrollo asistido por IA**, revisión de código generado, testing y documentación de entregables.

## Contexto — Qué leer antes de planificar

### 1. Lineamientos oficiales (extraídos de `Ejercicio Técnico Full Stack con IA Asistida.pdf`)

**Objetivo:** evaluar uso de IA como apoyo, análisis de código generado, identificación de mejoras y modificaciones funcionales.

**Premisa funcional:** foro con crear mensajes principales, responder, visualizar jerárquico, respuestas anidadas multinivel. Referencia: `http://leonidasesteban.github.io/react-discussions/`.

**Tecnologías:** `Front: Angular | Back: Java | Persistencia: Archivo JSON | VCS: GitHub | IA: Copilot, ChatGPT, Claude, Gemini, Cursor, etc.`

**Actividades:**

*Parte 1 – Generación con IA:* generar primera versión funcional; documentar herramienta(s), prompts, refinamiento y decisiones.

*Parte 2 – Análisis del código generado:* explicar arquitectura (componentes, flujo Front↔Back, manejo de datos, persistencia) y funcionamiento (creación/almacenamiento/renderizado de niveles/respuestas, relación padre-hijo `parentId`).

*Parte 3 – Mejoras:* identificar ≥2 oportunidades (problema, riesgo, solución, beneficio) entre rendimiento, escalabilidad, mantenibilidad, seguridad, diseño de componentes, estado, validaciones, pruebas, tipado, estructura de datos.

*Parte 4 – Cambio funcional:* primera versión con nivel fijo definido por IA; luego modificar para que el número máximo de niveles sea configurable por el propietario de la discusión (ej. `Discusión=0, direct=1, reply=2…`; `maxReplyDepth=3` permite hasta nivel 3; `null` ilimitado). Explicar qué partes se modificaron, por qué y qué implicaciones tienen.

**Entregables:** repo GitHub (código, historial, instrucciones) + `README.md` con:
1. Uso de IA (herramientas, prompts, aprendizajes).
2. Explicación de la solución (arquitectura, componentes, flujo).
3. Mejoras identificadas (≥2 justificadas).
4. Cambio funcional (explicación de niveles configurables).
5. Retos encontrados (problemas y cómo se resolvieron).

**Criterios/pesos:** Comprensión del código 30%, Calidad de la solución 20%, Capacidad de análisis y mejora 20%, Implementación del cambio funcional 20%, Comunicación y documentación 10%.

**Consideraciones finales:** uso de IA permitido pero debe poder explicarse/modificarse el código; se preguntará en revisión técnica.

### 2. Estado actual del repositorio (no modificar sin autorización)

**Monorepo:** `forum.test/` con `api/` (Spring Boot) + `app/` (Angular) + `bruno/` + `docs/` + `marvel-test.sh`.

**Versiones y entorno en la máquina:**
- `~/.m2` pertenece a `root`; se usa `MAVEN_USER_HOME=~/.local/share/maven` + `MAVEN_OPTS=-Dmaven.repo.local=...`.
- `JAVA_HOME=~/.local/opt/jdk-21` (Temurin 21.0.12.1, Java 21).
- `~/.local/opt/jdk-21` sin `sudo`.
- Node `22.22.3`, gestor **pnpm 12.4.1** (`packageManager: pnpm@12.4.1`); `npm` no se usa.

**API (Fase 1 — completada):**
- Stack: Spring Boot 4.1.1 (parent), Spring Security 7.1.1, Spring MVC 7, Validation (Jakarta), Lombok, DevTools, Jackson 3 (`tools.jackson.core:jackson-databind:3.1.5`, `DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS` en `jackson.databind.cfg`), `BCryptPasswordEncoder`.
- Puerto por defecto **`8081`** (8080 ocupado por `nginx`); `api/src/main/resources/application.yml` con `server.port: 8081`, `forum.data-dir: ./data`, `forum.default-max-reply-depth: 3`, `forum.cors-allowed-origins: [http://localhost:4200]`, `target/test-data` para perfil `test`.
- Persistencia: `JsonFileRepository<T,ID>` genérico con `Function<T,ID>`, `JavaType`, `ReentrantReadWriteLock` por archivo + archivo temporal y `Files.move(ATOMIC_MOVE)` con fallback; cada entidad en su archivo (`users.json`, `discussions.json`, `comments.json`, `tokens.json`); comentarios planos con `parentId`, árbol en lectura (`CommentService.buildTree` agrupando por `parentId` y ordenando por `createdAt`).
- Seguridad: token opaco `UUID` en `tokens.json`, `TokenAuthenticationFilter extends OncePerRequestFilter` → `UsernamePasswordAuthenticationToken(User)`, `SecurityFilterChain` stateless, CSRF off, CORS on, `RestAuthenticationEntryPoint` 401 / `RestAccessDeniedHandler` 403 escriben `ApiErrorResponse` vía `JsonMapper`.
- Endpoints 13:
  `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/logout`,
  `GET /api/users/me`, `GET /api/users/me/settings`, `PATCH /api/users/me/settings` (`UpdateUserSettingsRequest` con `Optional<Integer> maxReplyDepth` para distinguir `absent→null` vs `null→Optional.empty`),
  `GET /api/discussions`, `POST /api/discussions`, `GET /api/discussions/{id}` (incluye `maxReplyDepth` del dueño + árbol), `GET /api/users/me/discussions`, `GET /api/users/me/participating` (participando = comentó y no es suya),
  `POST /api/discussions/{id}/comments` (422 `Maximum reply depth exceeded`), `GET /api/discussions/{id}/comments`.
- Modelos: `User(id, username, passwordHash, maxReplyDepth, createdAt)` (username normalizado `trim`+`lowerCase`+sin espacios, `@Size 3..30`, único, `@Pattern` validado tras normalizar), `Discussion(id,title,content,authorId,createdAt)` (`title` max 150, `content` max 10000), `Comment(id,discussionId,parentId,authorId,content,createdAt)` (`content` max 5000), `AuthToken(token,userId,createdAt)`.
- Pruebas API: **44** (`ForumApiApplicationTests` 1, `JsonFileRepositoryTest` 9, `UpdateUserSettingsRequestTest` 3, `AuthServiceTest` 7, `UserServiceTest` 6, `DiscussionServiceTest` 6, `CommentServiceTest` 8, `ForumApiIntegrationTest` 4).
- Scripts: `marvel-test.sh` (tony/natasha/bruce secret123, 3 discusiones Marvel, niveles 3/5/ilimitado).

**App (Fase 2 — en curso, casi completa):**
- Stack: Angular **22.1.6** (standalone, zoneless, OnPush por defecto), Angular CLI 22.1.8, TypeScript 6.0, Tailwind CSS 4.1.12 CSS-first (`@import 'tailwindcss'` + `@theme` en `src/styles.css`), Vitest 4.1.11 (`testRunner: vitest`), pnpm 12.4.1, zoneless, file-name-style-guide 2025, `proxy.conf.json` (`/api`→8081).
- Core: `core/api.ts` (13 endpoints), `core/auth.ts` (signals + `localStorage` `forum_token`/`forum_user`, normaliza usuario), `core/token.interceptor.ts` (Bearer, limpia en 401), `core/auth.guard.ts`/`guest.guard.ts` (redirect `?returnUrl` saneada), `core/notification.ts` + `shared/notification-host.ts` (overlay centrado arriba, 3s, `success/error`).
- Features: `auth/` (Signal Forms `form`, `FormField`, `required`, `minLength`, `pattern`), `discussions/` (list con tabs `Todas/Mías/Participando`, detail con árbol, create), `comments/` (`comment-tree` recursivo con `canReplyAt(maxReplyDepth, level)` + `comment-composer` con `showVolver`), `settings/` (cards 3/5/ilimitado con `hasChanges`).
- UI: header `Discusiones` en `bg-ink #0a2240` con dropdown `Configuración`/`Cerrar sesión`, tabs y botones primarios `bg-accent`, tema `@theme` (`--color-ink`, `--color-accent`, etc.), `shared/app-header`, `shared/relative-time`, `shared/depth.ts`.
- Pruebas app: `app.spec.ts` + `depth.spec.ts` (8 tests Vitest).

**Docs actuales:**
- `docs/prompts/02-api-design.md` (contrato REST), `03-app-design.md` (frontend).
- `docs/plans/phase-1-api-plan.md` (histórico), `phase-2-app-plan.md` (estado parcial).
- `api/AGENTS.md`, `app/AGENTS.md`, `README.md` (con desfases 42→44).

## Lo que debes producir

### Salida 1 — Prompt (este archivo)
Este prompt debe ser reutilizable, autocontenido y profesional. Define rol, contexto, entregables y criterios, y sirve para regenerar/actualizar los planes sin depender del PDF externo.

### Salida 2 — `docs/plans/phase-1-api-plan.md`
Estado final detallado y preciso de la Fase 1, reflejando **lo ya construido** (no lo planificado originalmente):
- Arquitectura, modelo de datos, 13 endpoints, persistencia, seguridad, regla de profundidad, 44 tests, mejoras y mapeo a los entregables del PDF.
- Sección “Mejoras identificadas” (≥2) y “Cambio funcional” (backend + frontend) ya implementados.
- Prácticas aplicadas (Jackson 3, lock atómico, overlay alerts, usuario normalizado, etc.).

### Salida 3 — `docs/plans/phase-2-app-plan.md`
Estado final de la Fase 2 (Angular 22 + Tailwind + Signal Forms + Vitest): stack exacto, `core/`/`features/`/`shared/`, 13 endpoints, guards/interceptor, notificaciones overlay centradas, paleta `#0a2240`/`#EEF47A`, tabs, header, testing, y pasos reproducibles (9 pasos con commits) ya ejecutados.

## Restricciones para los planes
- No modificar código; solo documentar.
- Conventional Commits; cada commit y push requiere aprobación (excepto docs).
- Puerto documentado **8081**; `api/data/*.json` gitignoreado, `seed/` trackeado.
- No usar `withFetch()` (deprecado); no usar SCSS (Tailwind CSS-first).
- No añadir comentarios al código.

## Verificación de los planes
- Deben permitir reproducir el repo desde cero y entender el cambio funcional de niveles configurables.
- Deben mapear explícitamente cada sección del `README.md` exigida por el PDF (uso de IA, arquitectura, mejoras, cambio funcional, retos).
- Deben incluir criterios de evaluación (pesos) y consideraciones finales.

## Formato
Markdown profesional, con índice, tablas, ejemplos de request/response y fragmentos de código solo si aclaran la decisión.
