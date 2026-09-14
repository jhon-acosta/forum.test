# forum.test — Foro con niveles configurables de anidación

Prueba técnica Full Stack con IA asistida. Monorepo `api/` (Spring Boot) + `app/` (Angular) con persistencia en archivos JSON y nivel de anidación configurable por el dueño de la discusión.

> **Estado actual:** Fase 1 (API) **completada y probada** (`./mvnw test` 44/44) en **http://localhost:8081**. Fase 2 (Angular 22 + Tailwind) **completada** (auth con usuario normalizado, discusiones con tabs Todas/Mías/Participando, comentarios anidados con profundidad configurable, header `#0a2240` + notificaciones overlay centradas).

---

## Índice

- [Requisitos](#requisitos)
- [Ejecución](#ejecución)
- [Modelo de datos](#modelo-de-datos)
- [Configuración](#configuración)
- [Arquitectura](#arquitectura)
- [Referencia de la API](#referencia-de-la-api)
- [Persistencia y seguridad](#persistencia-y-seguridad)
- [Pruebas](#pruebas)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Uso de IA](#uso-de-ia)
- [Explicación de la solución](#explicación-de-la-solución)
- [Mejoras identificadas](#mejoras-identificadas)
- [Cambio funcional — niveles configurables](#cambio-funcional--niveles-configurables)
- [Retos encontrados](#retos-encontrados)
- [Fase 2 — App Angular](#fase-2--app-angular-en-curso)

---

## Requisitos

- **Java 21** (Temurin). En esta máquina `~/.local/opt/jdk-21`; `~/.bashrc` ya exporta `JAVA_HOME`, `PATH`, `MAVEN_USER_HOME` y `MAVEN_OPTS`. Maven Wrapper (`api/mvnw`) no requiere Maven instalado.
- **Node 22.22.3** y **pnpm 12.4.1** (para Fase 2, `packageManager: pnpm@12.4.1`).
- Git, `jq` (opcional, para `marvel-test.sh`), Bruno (opcional).

Si usas otro equipo con `JAVA_HOME` sin configurar:

```bash
export JAVA_HOME="$HOME/.local/opt/jdk-21"
export PATH="$JAVA_HOME/bin:$PATH"
export MAVEN_USER_HOME="$HOME/.local/share/maven"
export MAVEN_OPTS="-Dmaven.repo.local=$HOME/.local/share/maven/repository"
```

En esta máquina `~/.m2` pertenece a `root`, por eso se usa `MAVEN_USER_HOME` local.

---

## Ejecución

### API (Spring Boot) — puerto 8081

```bash
cd api
./mvnw spring-boot:run
# http://localhost:8081
# Datos en api/data/*.json (users, discussions, comments, tokens)
# Perfiles: default -> forum.data-dir=./data ; test -> target/test-data
```

Si necesitas otro puerto (8080 está ocupado por nginx en esta máquina):

```bash
SERVER_PORT=8081 ./mvnw spring-boot:run
# o: ./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

### Pruebas

```bash
cd api
./mvnw test
# 44 tests: 1 smoke + 9 JsonFileRepository + 3 Settings deserialización + 7 Auth + 6 User + 6 Discussion + 8 Comment + 4 E2E
cd app
pnpm run build && npx ng test --watch=false
# 8 tests Vitest (depth + app)
```

### Bruno (colección HTTP)

```bash
# Abrir bruno/forum-api en Bruno (https://www.usebruno.com)
# Environment: local (host=http://localhost:8081)
# Flujo: 01 Register -> 02 Login (guarda {{token}}) -> 03 Create Discussion -> 06 Create Comment -> 07 Reply -> 08/09 Settings -> 10 Logout
```

### Script Marvel listo para pegar

```bash
./marvel-test.sh  # registra tony/natasha/bruce, 3 discusiones Marvel, nivel 1→3, 422→ilimitado, árbol
```

### Frontend (Fase 2, completada)

```bash
cd app
pnpm install
pnpm start  # http://localhost:4200 (proxy /api -> 8081)
```

---

## Modelo de datos

Todo `id` es `UUID` generado por la app (nunca `_id` ni ObjectId). Fechas `LocalDateTime` en ISO-8601 (`2026-09-12T18:14:49.34`) gracias a Jackson 3 (`tools.jackson` + `DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS=false`).

### User — `api/data/users.json`

| Campo | Tipo | Restricciones / Notas |
|-------|------|------------------------|
| `id` | `UUID` | generado |
| `username` | `String` | `@NotBlank @Size(3..30)`, **único**, `trim()` |
| `passwordHash` | `String` | BCrypt, **nunca expuesto** en DTOs |
| `maxReplyDepth` | `Integer \| null` | `>=0`, default `3` (de `ForumProperties`), `null`=ilimitado. Pertenece al dueño de la discusión |
| `createdAt` | `LocalDateTime` | ISO-8601 |

```json
{
  "id": "a842d86a-6525-4282-8984-e5ffe2bc7178",
  "username": "tony",
  "maxReplyDepth": 3,
  "createdAt": "2026-09-12T18:14:49.344795858"
}
```

### Discussion — `api/data/discussions.json`

| Campo | Tipo | Restricciones |
|-------|------|---------------|
| `id` | `UUID` | generado |
| `title` | `String` | `@NotBlank @Size(max 150)`, `trim()` |
| `content` | `String` | `@NotBlank @Size(max 10000)`, `trim()` |
| `authorId` | `UUID` | FK → `User.id` |
| `createdAt` | `LocalDateTime` | ISO-8601 |

### Comment — `api/data/comments.json` (plano)

| Campo | Tipo | Restricciones |
|-------|------|---------------|
| `id` | `UUID` | generado |
| `discussionId` | `UUID` | FK → `Discussion.id` |
| `parentId` | `UUID \| null` | `null`=directa a discusión; si no null debe existir y pertenecer a la misma `discussionId` |
| `authorId` | `UUID` | FK → `User.id` |
| `content` | `String` | `@NotBlank @Size(max 5000)`, `trim()` |
| `createdAt` | `LocalDateTime` | ISO-8601 |

Regla de nivel: `discusión=0, direct=1, reply=2…`. El árbol se construye en lectura.

### AuthToken — `api/data/tokens.json`

| Campo | Tipo | Notas |
|-------|------|-------|
| `token` | `String` (UUID) | opaco, `Authorization: Bearer <token>` |
| `userId` | `UUID` | FK → `User.id` |
| `createdAt` | `LocalDateTime` | sin expiración en esta versión |

### DTOs expuestos (nunca `passwordHash`)

`RegisterRequest{username,password}`, `LoginRequest{username,password}`, `AuthResponse{token, user:UserResponse}`, `UserResponse{id,username,maxReplyDepth,createdAt}`, `AuthorResponse{id,username}`, `SettingsResponse{maxReplyDepth}`, `UpdateUserSettingsRequest{Optional<Integer> maxReplyDepth}`, `CreateDiscussionRequest{title,content}`, `DiscussionSummary{id,title,content,author,commentCount,createdAt}`, `DiscussionResponse{id,title,content,author,maxReplyDepth,createdAt,comments:[CommentResponse]}`, `CreateCommentRequest{content,parentId?}`, `CommentResponse{id,parentId,content,author,createdAt,replies:[]}`.

---

## Configuración

`api/src/main/resources/application.yml` (perfil `default`):

```yaml
server:
  port: 8081
forum:
  data-dir: ./data
  default-max-reply-depth: 3
  cors-allowed-origins:
    - http://localhost:4200
```

`api/src/test/resources/application-test.yml`:

```yaml
forum:
  data-dir: target/test-data
```

`ForumProperties` (`@ConfigurationProperties(prefix="forum")`) es un `record(Path dataDir, int defaultMaxReplyDepth, List<String> corsAllowedOrigins)` con `@ConfigurationPropertiesScan`. Jackson 3 se configura en `JacksonConfig` vía `JsonMapperBuilderCustomizer` (`disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)`, `enable(INDENT_OUTPUT)`, `disable(FAIL_ON_UNKNOWN_PROPERTIES)`).

---

## Arquitectura

**Modular monolith** (`controller → service → repository → JSON`), sin base de datos, microservicios ni Docker (requisito).

```
Angular (Fase 2, :4200)
   ↓ HTTP + Authorization: Bearer <token> (CORS 4200→8081)
Spring Boot 4.1.1 (Java 21) + Spring Security 7.1.1 + Jackson 3 (tools.jackson)
   ├─ controller: AuthController, UserController, DiscussionController, CommentController
   ├─ service: AuthService, TokenService, UserService, DiscussionService, CommentService
   ├─ repository: JsonFileRepository<T,ID> + UserRepository, DiscussionRepository, CommentRepository, TokenRepository
   ├─ config: ForumProperties, JacksonConfig, CorsConfig, SecurityConfig, TokenAuthenticationFilter, RestAuthenticationEntryPoint, RestAccessDeniedHandler
   ├─ dto/* + exception/GlobalExceptionHandler
   └─ data/*.json
```

---

## Referencia de la API

Base `http://localhost:8081`. Todos los endpoints bajo `/api` salvo `/error`. `Auth: Bearer` requerido salvo `register/login`.

### Autenticación

#### POST /api/auth/register — Registro

- **Auth:** no — **Body:** `RegisterRequest`

```json
{"username":"tony","password":"secret123"}
```

Validaciones: `username` 3..30 `NotBlank`, único; `password` 6..100 `NotBlank`.

- **201** `UserResponse`

```json
{"id":"a842d86a-...","username":"tony","maxReplyDepth":3,"createdAt":"2026-09-12T18:14:49.34"}
```

- **400** validación, **409** `Username already exists`.

#### POST /api/auth/login — Login

- **Body:** `LoginRequest` `{"username":"tony","password":"secret123"}` (ambos `NotBlank`)
- **200** `AuthResponse`

```json
{"token":"5dce2ac4-...","user":{"id":"a842d86a-...","username":"tony","maxReplyDepth":3,"createdAt":"..."}}
```

- **401** `Invalid credentials` (usuario no existe o password no coincide).

#### POST /api/auth/logout — Logout

- **Auth:** sí — **Header:** `Authorization: Bearer <token>`
- **204** sin body (revoca `tokens.json`). Idempotente; header ausente también 204 si ya autenticado.

### Usuarios (`@AuthenticationPrincipal User`)

#### GET /api/users/me

- **200** `UserResponse` del token.

#### GET /api/users/me/settings

- **200** `SettingsResponse`

```json
{"maxReplyDepth":3}
```

`null` → ilimitado (serializado como `null`).

#### PATCH /api/users/me/settings — Cambio funcional clave

- **Body:** `UpdateUserSettingsRequest` (clase mutable con `Optional<Integer> maxReplyDepth`)

```json
{"maxReplyDepth":5}    // limitado a 5
{"maxReplyDepth":null} // ilimitado
{}                     // ausente -> no cambia (PATCH)
```

Distinción Jackson 3: `absent→null` (no cambia), `null→Optional.empty` (ilimitado), `valor→Optional.of`. Ver `UpdateUserSettingsRequestTest`.

Validación: si presente y no null, `>=0` else `400 maxReplyDepth must be zero or greater`.

- **200** `SettingsResponse` actualizado.
- **401** sin auth.

### Discusiones

#### GET /api/discussions — Listar

- **200** `[DiscussionSummary]` orden `createdAt` desc.

```json
[{"id":"...","title":"Iron Man","content":"...","author":{"id":"...","username":"tony"},"commentCount":5,"createdAt":"..."}]
```

`commentCount` = `commentRepository.findByDiscussionId(id).size()`.

#### POST /api/discussions — Crear

- **Body:** `CreateDiscussionRequest` `{"title":"Hello","content":"World"}` (`title` max150, `content` max10000)
- **201** `DiscussionResponse` (con `comments:[]`)
- **400** validación.

#### GET /api/discussions/{id} — Detalle con árbol

- **200** `DiscussionResponse`

```json
{
  "id":"...","title":"Hello","content":"World",
  "author":{"id":"...","username":"tony"},"createdAt":"...",
  "comments":[
    {"id":"...","parentId":null,"content":"First","author":{"id":"...","username":"natasha"},"createdAt":"...","replies":[
      {"id":"...","parentId":"...","content":"Reply","author":{...},"createdAt":"...","replies":[]}
    ]}
  ]
}
```

Árbol construido en `CommentService.buildTree` agrupando por `parentId`, ordenando por `createdAt`. **404** si `id` no existe o autor no existe; **400** si `id` no es UUID.

#### GET /api/users/me/discussions — Propias

- **200** `[DiscussionSummary]` filtradas por `authorId` del principal, orden desc. Prestada por `UserController` (`/api/users/me/discussions`).

#### GET /api/users/me/participating — Participando

- **200** `[DiscussionSummary]` donde el usuario ha comentado y **no es autor** (sin duplicados, orden `createdAt` desc). Usado por la pestaña “Participando”.

### Comentarios

#### POST /api/discussions/{discussionId}/comments — Crear

- **Body:** `CreateCommentRequest` `{"content":"Hello","parentId":null}` (`content` max5000, `parentId` opcional UUID)
- **201** `CommentResponse` (con `replies:[]`)
- **400** `Parent comment does not belong to this discussion`, `Cycle detected`, `parentId` no es UUID
- **404** `Discussion not found`, `Parent comment not found`, `Discussion owner not found`, `Author not found`
- **422** `Maximum reply depth exceeded` (nivel `> maxReplyDepth` del dueño)
- **401** sin auth. Nivel: `discusión=0, direct=1, reply=2…`.

#### GET /api/discussions/{discussionId}/comments — Árbol plano (alternativo a `GET /api/discussions/{id}`)

- **200** `[CommentResponse]` árbol anidado (mismo `buildTree`).

### Errores — formato uniforme

```json
{
  "timestamp": "2026-09-12T18:14:50.006Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Maximum reply depth exceeded",
  "path": "/api/discussions/e84bc1c3-.../comments",
  "details": [{"field":"title","message":"must not be blank"}]
}
```

Manejado en `GlobalExceptionHandler` para `ApiException` (409/404/422…), `MethodArgumentNotValidException` (400 con `details`), `HttpMessageNotReadableException` (400), `MethodArgumentTypeMismatchException` (400), `AccessDenied/Authentication` (403/401), `UncheckedIOException/JacksonException` (500).

---

## Persistencia y seguridad

**Persistencia:** `JsonFileRepository<T,ID>` genérico con `Function<T,ID>`, `JavaType` (`constructCollectionType`), `ReentrantReadWriteLock` por archivo + archivo temporal y `Files.move(ATOMIC_MOVE, REPLACE_EXISTING)` con fallback a `REPLACE_EXISTING`. Lecturas tolerantes a archivo ausente/vacío y `createDirectories`. Comentarios planos con `parentId`; árbol en lectura.

**Seguridad:** `BCryptPasswordEncoder` (nunca exponer `passwordHash`). Token opaco UUID en `tokens.json`; `TokenAuthenticationFilter` (`OncePerRequestFilter`) lee `Authorization: Bearer`, resuelve `TokenService` → `UserRepository` y fija `UsernamePasswordAuthenticationToken(User, null, [])` en `SecurityContext` (`@AuthenticationPrincipal`). `SecurityFilterChain` stateless, CSRF off, CORS on (`CorsConfigurationSource` con `forum.cors-allowed-origins`), `httpBasic`/`formLogin` off. `RestAuthenticationEntryPoint` (401) y `RestAccessDeniedHandler` (403) escriben `ApiErrorResponse` vía `JsonMapper.writeValueAsString`.

---

## Pruebas

`./mvnw test` usa `target/test-data` (perfil `test`) y `JsonMapper` aislado para repositorios.

| Test | Qué cubre |
|------|-----------|
| `ForumApiApplicationTests` | Contexto arranca y excluye `UserDetailsServiceAutoConfiguration` |
| `JsonFileRepositoryTest` (9) | Atomicidad, `ReentrantReadWriteLock` concurrente (8×10), archivo vacío, visibilidad entre instancias, fechas ISO |
| `UpdateUserSettingsRequestTest` (3) | Jackson 3: `absent→null`, `null→Optional.empty`, valor→`Optional.of` |
| `AuthServiceTest` (7) | BCrypt, duplicado→409, login válido/inválido, logout |
| `UserServiceTest` (6) | `getById`, 3 semánticas de settings, negativo→400 |
| `DiscussionServiceTest` (6) | Crear, orden desc + `commentCount`, `findById` con `CommentService.findTree`, propias, `findParticipating` sin propias |
| `CommentServiceTest` (8) | Niveles 1/2/3 con `maxDepth=3`, 4→422, `null` ilimitado, padre otra discusión/inexistente, discusión inexistente, árbol |
| `ForumApiIntegrationTest` (4) | `@SpringBootTest` + `MockMvc` en `:8081`: flujo E2E (register→login→discussion→comment→reply hasta 3→422→PATCH ilimitado→nivel 4/5→árbol + `participatingExcludesOwnDiscussions`), duplicado→409, login erróneo→401. `cleanData()` borra `target/test-data` por `BeforeEach` |

---

## Estructura del proyecto

```
forum.test/
├── README.md
├── .gitignore
├── marvel-test.sh
├── api/
│   ├── AGENTS.md
│   ├── pom.xml
│   ├── mvnw / .mvn/wrapper/
│   ├── src/main/java/com/forum/api/{config,model,dto,repository,service,controller,exception}
│   │   ├── config/ ForumProperties, JacksonConfig, CorsConfig, SecurityConfig, TokenAuthenticationFilter, RestAuthenticationEntryPoint, RestAccessDeniedHandler
│   │   ├── model/ User, Discussion, Comment, AuthToken
│   │   ├── dto/auth|user|discussion|comment|error
│   │   ├── repository/ JsonFileRepository, UserRepository, DiscussionRepository, CommentRepository, TokenRepository
│   │   ├── service/ AuthService, TokenService, UserService, DiscussionService, CommentService
│   │   ├── controller/ AuthController, UserController, DiscussionController, CommentController
│   │   └── exception/ ApiException, GlobalExceptionHandler
│   ├── src/main/resources/application.yml
│   ├── src/test/resources/application-test.yml
│   ├── src/test/java/com/forum/api/**Test.java
│   └── data/.gitkeep
├── app/
│   └── AGENTS.md
├── bruno/forum-api/    # colección Bruno 10 requests + local env (host 8081)
└── docs/
    ├── prompts/02-api-design.md
    └── plans/phase-1-api-plan.md
```

Historial en **Conventional Commits**; cada commit/push requirió aprobación explícita.

---

## Uso de IA

**Herramientas:** Muse Spark (OpenCode) como asistente principal. La evaluación permitía Copilot/ChatGPT/Claude/Gemini/Cursor, etc.; aquí se delegó el scaffolding y la generación inicial a la IA y se refinó manualmente.

**Prompts principales:**

- *“Diseño del API REST”* (`docs/prompts/02-api-design.md`): contrato completo (entidades, DTOs, endpoints, validaciones, códigos HTTP, persistencia, profundidad, estructura de paquetes y flujos) antes de escribir código.
- *“Genera starter Spring Boot desde start.spring.io con web/validation/security/lombok/configuration-processor/devtools”* y adaptación a Jackson 3.
- *“Implementa JsonFileRepository genérico con lock + escritura atómica y repository concretos”* y sus pruebas.
- *“En Jackson 3 `WRITE_DATES_AS_TIMESTAMPS` es `DateTimeFeature`, no `SerializationFeature`; `JsonMapper` es el mapper; `jackson-databind-nullable` no es compatible”* — corrección del modelo sugerido por la IA.

**Refinamiento:** El plan original proponía `jackson-databind-nullable` (`JsonNullable`) para `PATCH` `null` vs ausente. Al detectar el cambio a Jackson 3 (Boot 4.1.1 usa `tools.jackson`), se sustituyó por una **clase mutable con `Optional<Integer>`** y se verificó empíricamente (`OptionalProbeTest`): en un record ambos casos son `Optional.empty`; en una clase con setter `absent→null`, `null→Optional.empty`. Se documentó con `UpdateUserSettingsRequestTest` y se mantuvo el helper `isProvided()/isUnlimited()`. También se fijó `MAVEN_USER_HOME` local porque `~/.m2` pertenecía a `root` y `AutoConfigureMockMvc` cambió a `org.springframework.boot.webmvc.test.autoconfigure`.

**Aprendizajes:**

- Boot 4 migra a Jackson 3 (`tools.jackson`), incompatibilizando librerías de Jackson 2.
- `JsonMapperBuilderCustomizer` + `DateTimeFeature` en Boot 4 difiere de Boot 3.
- `AutoConfigureMockMvc` cambió de paquete.
- Con `@TempDir` y `JsonMapper.builder()` se prueban repositorios sin Spring; con `MockMvc` + `target/test-data` se prueba el flujo real.
- El puerto por defecto pasó a **8081** porque 8080 lo ocupa nginx en esta máquina.

---

## Explicación de la solución

**Componentes:** `User` (`maxReplyDepth`), `Discussion` (`authorId`), `Comment` (`discussionId`, `parentId` nullable), `AuthToken`. Relaciones por `UUID`.

**Flujo Front ↔ Back:** Angular → `POST /api/auth/register` → BCrypt → `users.json` → `POST /api/auth/login` → token opaco en `tokens.json` → `Authorization: Bearer` → `TokenAuthenticationFilter` → `SecurityContext` → controllers → services → repositories.

**Manejo de datos:** DTOs separan modelo interno de HTTP; `passwordHash` nunca se expone. `UpdateUserSettingsRequest` es el único DTO mutable.

**Creación de comentarios:** `CommentService.create` valida discusión, `parentId` y pertenencia, resuelve dueño → `maxReplyDepth`, calcula nivel caminando `parentId` con `visited` anti-ciclo, rechaza si excede, genera `UUID` y guarda.

**Almacenamiento:** Plano en `comments.json`. **Renderizado:** `CommentService.buildTree` agrupa por `parentId`, ordena por `createdAt` y recursa: `GET /api/discussions/{id}` devuelve `DiscussionResponse { comments: [ {replies:[...]} ] }`.

**Relaciones padre-hijo:** `parentId=null` = hija directa; `parentId=UUID` = reply.

---

## Mejoras identificadas

**1) Concurrencia y atomicidad de la persistencia en JSON**

- *Problema:* Escrituras simultáneas corrompen el archivo.
- *Riesgo:* Pérdida de datos y 500 bajo concurrencia.
- *Solución implementada:* `ReentrantReadWriteLock` + archivo temporal y `Files.move(ATOMIC_MOVE)` con fallback; lectura tolerante a ausente/vacío. Ver `JsonFileRepository` y `JsonFileRepositoryTest.concurrentSavesKeepFileConsistent`.
- *Beneficio:* Integridad con costo mínimo; tests lo verifican.

**2) Manejo de errores y validaciones**

- *Problema:* Scaffold solo 500 sin detalles.
- *Riesgo:* Frontend no distingue 409 vs 422 vs 400.
- *Solución implementada:* Bean Validation + `{timestamp,status,error,message,path,details[]}` en `GlobalExceptionHandler` para `MethodArgumentNotValidException`, `HttpMessageNotReadable`, `MethodArgumentTypeMismatch`, `ApiException`, `AccessDenied/Authentication`, `UncheckedIOException/JacksonException`; `RestAuthenticationEntryPoint` y `CorsConfig`.
- *Beneficio:* Contratos predecibles por campo.

*Pendientes:* paginación en `GET /api/discussions`, índice en memoria por `discussionId`, migrar `tokens.json` a JWT si escala.

---

## Cambio funcional — niveles configurables

La versión inicial sugería límite fijo. Ahora **cada dueño controla su límite** y puede cambiarlo en caliente.

- **Modelo:** `User.maxReplyDepth: Integer|null` (default `3`; `null`=ilimitado) con `ForumProperties.defaultMaxReplyDepth`.
- **Contrato:** `GET/PATCH /api/users/me/settings` con `{maxReplyDepth: number|null}`; `absent→null` (no cambia), `null→Optional.empty` (ilimitado).
- **Regla:** `discusión=0, direct=1`. `CommentService.computeNewLevel` camina la cadena con anti-ciclo y rechaza `422 Maximum reply depth exceeded` si `maxDepth!=null && newLevel>maxDepth`. Grandfathering para comentarios ya creados.

```
# maxReplyDepth=3
POST ... {content:"A"}               → 1 OK
POST ... {content:"B", parentId:A}   → 2 OK
POST ... {content:"C", parentId:B}   → 3 OK
POST ... {content:"D", parentId:C}   → 422

PATCH /api/users/me/settings {maxReplyDepth:5}
POST ... {content:"D", parentId:C}   → 4 OK
PATCH ... {maxReplyDepth:null}
POST ... {content:"E", parentId:D}   → 5 OK (ilimitado)
```

---

## Retos encontrados

**Máquina sin Java/Maven y `~/.m2` de `root`:** `mvnw` fallaba con `mkdir: Permission denied`. Temurin 21 en `~/.local/opt/jdk-21` sin `sudo` y `MAVEN_USER_HOME=~/.local/share/maven` + `MAVEN_OPTS=-Dmaven.repo.local=...`.

**Spring Boot 4 + Jackson 3:** `JsonMapper` + `DateTimeFeature` en lugar de `SerializationFeature`; `jackson-databind-nullable` incompatible; `AutoConfigureMockMvc` cambió de paquete. Se adaptó `JacksonConfig` y tests con `dependency:tree` y probes.

**Puerto 8080 ocupado por nginx (desde 2026-09-12 el default es 8081):** El error `Web server failed to start. Port 8080 was already in use` provenía de nginx, no del foro. Se fijó `server.port: 8081` y `bruno/.../local.bru` a `8081`; las pruebas E2E y `marvel-test.sh` usan `8081`.

---

## Fase 2 — App Angular (completada)

**Stack:** Angular **22.1.6** (standalone, zoneless, OnPush), **Tailwind CSS 4.1.12** CSS-first (`@import 'tailwindcss'` + `@theme` en `src/styles.css`), **TypeScript 6.0**, **Vitest 4.1.11**, **pnpm 12.4.1**.

**Estado:** Scaffold con `--style=tailwind --routing --ssr=false --zoneless --test-runner=vitest`, design tokens (`#0a2240` header, acento dorado), `proxy.conf.json` → `8081`, core con `AuthService` (usuario normalizado a minúsculas sin espacios), `tokenInterceptor`, `authGuard`/`guestGuard`, notificaciones overlay centradas (3 s), auth UI con **Signal Forms**, discusiones con **tabs `Todas (n)` · `Mías (n)` · `Participando (n)`**, detalle con árbol y `Volver` ↔ `Comentar`, creación con `Cancelar` ↔ `Crear`, comentarios con `maxReplyDepth` y header `Discusiones` con dropdown **Configuración** / **Cerrar sesión**.

**Completado:** discusiones, comentarios anidados, settings con `hasChanges` y tema Aval Buró (ver `docs/plans/phase-2-app-plan.md`).

Verificación final: `./mvnw test` **44/44** (API) y `pnpm run build` + `npx ng test --watch=false` (8 tests App) en verde.
