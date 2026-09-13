# Plan — Fase 1: API (Spring Boot) — Estado final

> Refleja lo ya construido (no lo planificado originalmente). Base para reproducir la Fase 1 y para la revisión técnica según los lineamientos del PDF.

## 1. Objetivo y alcance

Implementar el foro con persistencia en archivos JSON y nivel de anidación configurable por el propietario de la discusión (`maxReplyDepth` 3/5/`null`=ilimitado), con pruebas, manejo de errores y documentación que mapee los entregables del PDF.

## 2. Decisiones acordadas

| Tema | Decisión final |
|---|---|
| JDK | Java 21 Temurin en `~/.local/opt/jdk-21` (sin `sudo`) |
| Framework | Spring Boot 4.1.1 (Maven), `spring-boot-starter-parent` |
| Auth | Token opaco `UUID` en `Authorization: Bearer`, persistido en `tokens.json` (evolucionable a JWT) |
| Profundidad ilimitada | `null`; en `PATCH` se distingue `absent→null` (no cambia) vs `null→Optional.empty` (ilimitado) con clase mutable |
| Repositorio | Monorepo `forum.test/` con `api/`, `app/`, `bruno/`, `seed/`, `docs/` |
| Commits | Conventional Commits; cada commit y push requiere aprobación |
| Docs | `docs/prompts/` (02-api, 03-app, 04-master) y `docs/plans/` (fase 1 y 2) |

## 3. Hallazgos del entorno (resueltos)

- `java`, `mvn`, `gradle` no instalados → Temurin 21 tarball en `~/.local/opt/jdk-21` + `MAVEN_USER_HOME=~/.local/share/maven`.
- `~/.m2` pertenece a `root` → `MAVEN_OPTS=-Dmaven.repo.local=...`.
- Spring Boot 4.1.1 usa **Jackson 3** (`tools.jackson.core`), incompatible con `jackson-databind-nullable` → se usó `Optional<Integer>` en clase mutable.
- 8080 ocupado por `nginx` → `server.port: 8081`.

## 4. Arquitectura — Modular monolith

```
Angular -> HTTP/REST (Bearer) -> Spring Boot -> Controller -> Service -> Repository -> JSON (data/*.json)
```

Paquetes:

- `config/` — `ForumProperties` (`forum.data-dir`, `default-max-reply-depth=3`, `cors-allowed-origins`), `JacksonConfig` (`JsonMapperBuilderCustomizer` + `DateTimeFeature`), `CorsConfig` (`CorsConfigurationSource` para `/api/**`), `SecurityConfig`, `TokenAuthenticationFilter`, `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`.
- `model/` — `User(id, username, passwordHash, maxReplyDepth, createdAt)`, `Discussion(id, title, content, authorId, createdAt)`, `Comment(id, discussionId, parentId, authorId, content, createdAt)`, `AuthToken(token, userId, createdAt)`.
- `dto/` — `RegisterRequest`, `LoginRequest`, `AuthResponse`, `UserResponse`, `AuthorResponse`, `SettingsResponse`, `UpdateUserSettingsRequest(Optional<Integer>)`, `CreateDiscussionRequest`, `DiscussionSummary`, `DiscussionResponse(maxReplyDepth)`, `CreateCommentRequest`, `CommentResponse`, `ApiErrorResponse`, `FieldValidationError`.
- `repository/` — `JsonFileRepository<T,ID>` genérico (`ReentrantReadWriteLock` + archivo temporal + `Files.move(ATOMIC_MOVE)` con fallback; `createDirectories` en constructor) + `UserRepository`, `DiscussionRepository`, `CommentRepository`, `TokenRepository`.
- `service/` — `AuthService` (BCrypt + normalización de usuario), `TokenService`, `UserService`, `DiscussionService`, `CommentService`.
- `controller/` — `AuthController`, `UserController`, `DiscussionController`, `CommentController`.
- `exception/` — `ApiException`, `GlobalExceptionHandler`.

## 5. Modelo de datos y validaciones (preciso)

- **User:** `username` `@NotBlank @Size(3..30)` + normalizado `trim`+`lowerCase`+sin espacios, único, `^[a-z0-9._-]+$` validado tras normalizar; `password` `@NotBlank @Size(6..100)` → BCrypt; `maxReplyDepth` `Integer|null` `>=0` o `null`, default `3`.
- **Discussion:** `title` `max 150`, `content` `max 10000`, `authorId` FK a `User`.
- **Comment:** `content` `max 5000`, `parentId` nullable UUID que debe existir y pertenecer a la misma `discussionId`; nivel `discusión=0, direct=1…`.

## 6. Endpoints 13 (preciso)

| Método | Ruta | Auth | Request | Response | Códigos |
|---|---|---|---|---|---|
| POST | `/api/auth/register` | no | `{username,password}` | 201 `UserResponse` | 400, 409 |
| POST | `/api/auth/login` | no | `{username,password}` | 200 `AuthResponse` | 401 |
| POST | `/api/auth/logout` | sí | `Bearer` | 204 | 401 |
| GET | `/api/users/me` | sí | — | 200 `UserResponse` | 401 |
| GET | `/api/users/me/settings` | sí | — | 200 `SettingsResponse` | 401 |
| PATCH | `/api/users/me/settings` | sí | `{maxReplyDepth: number\|null}` | 200 `SettingsResponse` | 400, 401 |
| GET | `/api/discussions` | sí | — | 200 `[DiscussionSummary]` orden `createdAt` desc | 401 |
| POST | `/api/discussions` | sí | `{title,content}` | 201 `DiscussionResponse` | 400, 401 |
| GET | `/api/discussions/{id}` | sí | — | 200 `DiscussionResponse{maxReplyDepth, comments}` | 400, 401, 404 |
| GET | `/api/users/me/discussions` | sí | — | 200 `[DiscussionSummary]` | 401 |
| GET | `/api/users/me/participating` | sí | — | 200 `[DiscussionSummary]` (participando, no mías) | 401 |
| POST | `/api/discussions/{id}/comments` | sí | `{content,parentId?}` | 201 `CommentResponse` | 400, 401, 404, 422 |
| GET | `/api/discussions/{id}/comments` | sí | — | 200 `[CommentResponse]` árbol | 401, 404 |

Errores uniformes: `{timestamp, status, error, message, path, details[]}`.

## 7. Persistencia y seguridad

- `JsonFileRepository` abstracto con `Function<T,ID>` y `JavaType`; `findAll` tolera archivo ausente/vacío.
- `BCryptPasswordEncoder`, token opaco `UUID` en `tokens.json`, `TokenAuthenticationFilter extends OncePerRequestFilter` → `UsernamePasswordAuthenticationToken(User)` en `SecurityContext` (`@AuthenticationPrincipal User`), `SecurityFilterChain` stateless, CSRF off, CORS on, entry points 401/403 vía `JsonMapper`.

## 8. Pruebas API — 44

| Test | Qué cubre |
|---|---|
| `ForumApiApplicationTests` 1 | Contexto y exclusión de `UserDetailsServiceAutoConfiguration` |
| `JsonFileRepositoryTest` 9 | Atomicidad, concurrencia 8×10, archivo vacío, visibilidad, fechas ISO |
| `UpdateUserSettingsRequestTest` 3 | Jackson 3: `absent→null`, `null→empty`, valor |
| `AuthServiceTest` 7 | Normalización, duplicado 409, login válido/inválido |
| `UserServiceTest` 6 | `getById`, 3 semánticas de settings, negativo 400 |
| `DiscussionServiceTest` 6 | Crear, orden desc + `commentCount`, `findById` con `maxReplyDepth`, `findParticipating` sin propias |
| `CommentServiceTest` 8 | Niveles 3/5/`null`, 4→422, padre otra discusión/inexistente, árbol |
| `ForumApiIntegrationTest` 4 | E2E `422→PATCH null→nivel 4/5`, duplicado 409, login 401, `participatingExcludesOwnDiscussions` |

## 9. Pasos y commits ejecutados (resumen)

1. `chore: scaffold Spring Boot API project`
2. `chore(config): add application config and profiles`
3. `feat(domain): add core domain models`
4. `feat(persistence): add atomic JSON file repositories`
5. `feat(api): add DTOs, validation and global error handling`
6. `feat(auth): add opaque token authentication`
7. `feat(user): add profile and settings endpoints`
8. `feat(discussion): add discussion endpoints`
9. `feat(comment): add nested comments with configurable reply depth`
10. `test(api): add end-to-end tests and seed data`
11. `docs: add README, implementation plans and prompts`
12. `feat: hide reply action when discussion reply depth limit is reached`
13. `feat: add discussion tabs and header user menu`
14. `feat: normalize usernames and add centered overlay alerts` (actual)

## 10. Prácticas aplicadas

- DTOs nunca exponen `passwordHash`; `id` UUID, no `_id`.
- JSON separado por entidad, comentarios planos + `parentId`, árbol en lectura.
- `maxReplyDepth` del dueño, `null`=ilimitado, `0` sin comentarios, `grandfathering`.
- Validaciones Bean + manejo uniforme; CORS 4200→8081; `MAVEN_USER_HOME` local.
- Tests por feature; `target/test-data` para e2e; `marvel-test.sh` con 3 usuarios Marvel.

## 11. Retos y mapeo al PDF

- Mapeo: README §1–5 ↔ entregables 1–5; pesos de evaluación 30/20/20/20/10 cubiertos.
- Cambio funcional de niveles: `User.maxReplyDepth` + `PATCH` + `CommentService.computeNewLevel` + `DiscussionResponse.maxReplyDepth` + UI.
