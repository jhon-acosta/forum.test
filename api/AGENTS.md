# AGENTS.md — api (Spring Boot)

Guía para agentes de IA y desarrolladores que trabajen en el backend del foro.

## Stack

- **Java 21** (Temurin en `~/.local/opt/jdk-21`) + **Maven Wrapper** (`./mvnw`, no requiere `mvn` global).
- **Spring Boot 4.1.1**, **Spring Security 7.1.1**, **Spring MVC 7**, **Validation (Jakarta)**, **Lombok**, **DevTools**.
- **Jackson 3** (`tools.jackson.core:jackson-databind:3.1.5`) — Ojo: `com.fasterxml.jackson` (Jackson 2) no aplica; por eso `org.openapitools:jackson-databind-nullable` **no** es compatible. `WRITE_DATES_AS_TIMESTAMPS` es `tools.jackson.databind.cfg.DateTimeFeature`.
- Persistencia en archivos JSON (`data/users.json`, `discussions.json`, `comments.json`, `tokens.json`) — sin base de datos, microservicios ni Docker.
- Puerto por defecto **8081** (`server.port: 8081` en `src/main/resources/application.yml`; 8080 ocupado por nginx en esta máquina).

## Entorno en esta máquina

`~/.m2` pertenece a `root`, así que se usa Maven home local. Antes de compilar:

```bash
export JAVA_HOME="$HOME/.local/opt/jdk-21"
export PATH="$JAVA_HOME/bin:$PATH"
export MAVEN_USER_HOME="$HOME/.local/share/maven"
export MAVEN_OPTS="-Dmaven.repo.local=$HOME/.local/share/maven/repository"
```

En VS Code: `settings.json` con `"java.jdt.ls.java.home": "$HOME/.local/opt/jdk-21"`. Extensiones instaladas: `vscjava.vscode-java-pack` + `vmware.vscode-boot-dev-pack`.

## Comandos

```bash
./mvnw test                          # 42 tests (ver sección Pruebas)
./mvnw spring-boot:run               # http://localhost:8081 (data en ./data)
SERVER_PORT=8081 ./mvnw spring-boot:run  # override temporal
./mvnw clean package -DskipTests     # jar en target/forum-api-0.0.1-SNAPSHOT.jar
```

Colección HTTP: `bruno/forum-api/` (10 requests, env `local` con `host=http://localhost:8081`). Script `marvel-test.sh` registra `tony/natasha/bruce` y ejercita niveles.

## Arquitectura (modular monolith)

```
controller -> service -> repository -> JSON files
```

```
config/  ForumProperties, JacksonConfig (JsonMapperBuilderCustomizer), CorsConfig, SecurityConfig, TokenAuthenticationFilter, RestAuthenticationEntryPoint, RestAccessDeniedHandler
model/   User(id,username,passwordHash,maxReplyDepth,createdAt), Discussion(id,title,content,authorId,createdAt), Comment(id,discussionId,parentId,authorId,content,createdAt), AuthToken(token,userId,createdAt)
dto/     auth/RegisterRequest,LoginRequest,AuthResponse | user/UserResponse,AuthorResponse,SettingsResponse,UpdateUserSettingsRequest(Optional<Integer>) | discussion/CreateDiscussionRequest,DiscussionSummary,DiscussionResponse(maxReplyDepth) | comment/CreateCommentRequest,CommentResponse | error/ApiErrorResponse,FieldValidationError
repository/ JsonFileRepository<T,ID> + UserRepository, DiscussionRepository, CommentRepository, TokenRepository
service/  AuthService, TokenService, UserService, DiscussionService, CommentService
controller/ AuthController, UserController, DiscussionController, CommentController
exception/ ApiException, GlobalExceptionHandler (@RestControllerAdvice)
```

- `ForumProperties` (`forum.data-dir`, `default-max-reply-depth=3`, `cors-allowed-origins=[http://localhost:4200]`).
- `JacksonConfig` desactiva `DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS`, activa `INDENT_OUTPUT`, desactiva `FAIL_ON_UNKNOWN_PROPERTIES`.
- `CorsConfig` expone `CorsConfigurationSource` para `/api/**` con `AllowCredentials`.

## Modelo y validaciones (preciso)

- **User:** `username` normalizado `trim`+`toLowerCase`+sin espacios, `@NotBlank @Size(3..30)` y `^[a-z0-9._-]+$` (validado tras normalizar), único case-insensitive, `password` `@NotBlank @Size(6..100)` → BCrypt, `maxReplyDepth` `Integer|null` `>=0` o `null` (default 3).
- **Discussion:** `title` `@NotBlank @Size(max 150)`, `content` `@NotBlank @Size(max 10000)`.
- **Comment:** `content` `@NotBlank @Size(max 5000)`, `parentId` nullable UUID que debe existir y pertenecer a la misma `discussionId`.

## Endpoints expuestos (13)

| Método | Ruta | Auth | Request | Response | Códigos |
|--------|------|------|---------|----------|---------|
| POST | `/api/auth/register` | no | `{username,password}` | `201 UserResponse` | 400, 409 |
| POST | `/api/auth/login` | no | `{username,password}` | `200 {token,user}` | 401 |
| POST | `/api/auth/logout` | sí | `Authorization: Bearer` | `204` | 401 |
| GET | `/api/users/me` | sí | — | `200 UserResponse` | 401 |
| GET | `/api/users/me/settings` | sí | — | `200 {maxReplyDepth}` | 401 |
| PATCH | `/api/users/me/settings` | sí | `{maxReplyDepth:number\|null}` (absent=no cambia) | `200 {maxReplyDepth}` | 400, 401 |
| GET | `/api/discussions` | sí | — | `200 [DiscussionSummary]` | 401 |
| POST | `/api/discussions` | sí | `{title,content}` | `201 DiscussionResponse` | 400, 401 |
| GET | `/api/discussions/{id}` | sí | — | `200 DiscussionResponse{maxReplyDepth, comments:[CommentResponse]}` | 400, 401, 404 |
| GET | `/api/users/me/discussions` | sí | — | `200 [DiscussionSummary]` | 401 |
| GET | `/api/users/me/participating` | sí | — | `200 [DiscussionSummary]` (participando, no mías) | 401 |
| POST | `/api/discussions/{id}/comments` | sí | `{content,parentId?}` | `201 CommentResponse` | 400, 401, 404, 422 |
| GET | `/api/discussions/{id}/comments` | sí | — | `200 [CommentResponse]` árbol | 401, 404 |

Árbol: `CommentService.buildTree` agrupa por `parentId`, ordena por `createdAt`, recursa. Nivel `discusión=0, direct=1`. Errores en `{timestamp,status,error,message,path,details[]}` vía `GlobalExceptionHandler` (cubre `MethodArgumentTypeMismatch`, `HttpMessageNotReadable`, `AccessDenied`).

## Persistencia y seguridad (preciso)

- `JsonFileRepository<T,ID>` abstracto con `Function<T,ID>`, `JavaType`, `ReentrantReadWriteLock` y escritura a temp + `Files.move(ATOMIC_MOVE, REPLACE_EXISTING)` con fallback. Directorios creados en constructor.
- `UserRepository.findByUsername/existsByUsername`, `DiscussionRepository.findByAuthorId`, `CommentRepository.findByDiscussionId`, `TokenRepository(String)`.
- `BCryptPasswordEncoder`, token opaco `UUID.randomUUID().toString()` en `tokens.json`, `TokenAuthenticationFilter extends OncePerRequestFilter` → `UsernamePasswordAuthenticationToken(User, null, [])` en `SecurityContext` (`@AuthenticationPrincipal User`), `SecurityFilterChain` stateless, CSRF off, CORS on, `RestAuthenticationEntryPoint` 401 y `RestAccessDeniedHandler` 403 escriben `ApiErrorResponse` vía `JsonMapper.writeValueAsString`.

## Pruebas (44)

- `ForumApiApplicationTests` 1, `JsonFileRepositoryTest` 9, `UpdateUserSettingsRequestTest` 3, `AuthServiceTest` 7, `UserServiceTest` 6, `DiscussionServiceTest` 6, `CommentServiceTest` 8, `ForumApiIntegrationTest` 4 (incluye `participatingExcludesOwnDiscussions`).

## Convenciones

- **Conventional Commits** (`feat`, `fix`, `test`, `docs`, `chore`, `refactor`) con scopes `config|domain|persistence|api|auth|user|discussion|comment`.
- No agregar comentarios al código salvo que se soliciten.
- Cada feature incluye sus `*Test`.
- Commits y push requieren aprobación explícita del usuario.
- Puerto documentado: **8081** (`application.yml` + `bruno/**/local.bru`).
