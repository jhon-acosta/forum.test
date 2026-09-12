# forum.test — Foro con niveles configurables de anidación

Prueba técnica Full Stack con IA asistida. Monorepo `api/` (Spring Boot) + `app/` (Angular) con persistencia en archivos JSON y nivel de anidación configurable por el dueño de la discusión.

> **Estado actual:** Fase 1 (API) **completada y probada** (`./mvnw test` 42/42). Fase 2 (Angular) pendiente.

---

## Índice

- [Requisitos](#requisitos)
- [Ejecución](#ejecución)
- [API](#api)
- [Arquitectura](#arquitectura)
- [Persistencia y seguridad](#persistencia-y-seguridad)
- [Pruebas](#pruebas)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Uso de IA](#uso-de-ia)
- [Explicación de la solución](#explicación-de-la-solución)
- [Mejoras identificadas](#mejoras-identificadas)
- [Cambio funcional — niveles configurables](#cambio-funcional--niveles-configurables)
- [Retos encontrados](#retos-encontrados)

---

## Requisitos

- **Java 21** (Temurin). En esta máquina `~/.local/opt/jdk-21`; `~/.bashrc` ya exporta `JAVA_HOME` y `PATH`. Maven Wrapper (`api/mvnw`) no requiere Maven instalado.
- **Node 22** y **npm 10** (para Fase 2).
- Git.

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

### API (Spring Boot)

```bash
cd api
./mvnw spring-boot:run
# http://localhost:8080
# Datos en ./data/*.json (users, discussions, comments, tokens)
```

Perfiles: `default` usa `forum.data-dir=./data`; `test` usa `target/test-data`.

### Bruno (colección HTTP)

```bash
# Abrir bruno/forum-api en Bruno (https://www.usebruno.com)
# Environment: local (host=http://localhost:8080)
# 1. 01 Register -> 02 Login (guarda {{token}}) -> 03 Create Discussion -> 06 Create Comment -> 07 Reply -> etc.
```

### Pruebas

```bash
cd api
./mvnw test
# 42 tests: 1 smoke + 9 JsonFileRepository + 3 Settings deserialization + 7 Auth + 6 User + 5 Discussion + 8 Comment + 3 E2E
```

### Frontend (Fase 2, pendiente)

```bash
cd app
npx @angular/cli new . --routing --style=css  # cuando se retome
npm install
npm start  # http://localhost:4200
```

---

## API

Base `http://localhost:8080`.

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| `POST` | `/api/auth/register` | no | Registro `{username,password}` → 201 `UserResponse` |
| `POST` | `/api/auth/login` | no | Login `{username,password}` → 200 `{token, user}` |
| `POST` | `/api/auth/logout` | sí | Invalida el token → 204 |
| `GET` | `/api/users/me` | sí | Usuario autenticado |
| `GET` | `/api/users/me/settings` | sí | `{maxReplyDepth}` |
| `PATCH` | `/api/users/me/settings` | sí | `{maxReplyDepth: number\|null}` (ver [niveles](#cambio-funcional--niveles-configurables)) |
| `GET` | `/api/discussions` | sí | Lista de discusiones (orden `createdAt` desc, con `commentCount`) |
| `POST` | `/api/discussions` | sí | Crear discusión `{title,content}` → 201 |
| `GET` | `/api/discussions/{id}` | sí | Discusión + árbol de comentarios |
| `GET` | `/api/users/me/discussions` | sí | Discusiones propias |
| `POST` | `/api/discussions/{id}/comments` | sí | Crear comentario `{content, parentId?}` → 201 |
| `GET` | `/api/discussions/{id}/comments` | sí | Árbol de comentarios |

Errores en formato uniforme:

```json
{
  "timestamp": "2026-09-12T18:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/discussions",
  "details": [{"field":"title","message":"must not be blank"}]
}
```

Códigos: `201` creado (+ `Location` implícita), `204` sin contenido, `400` validación/JSON malformado/`parentId` de otra discusión, `401` sin `Bearer`, `403` denegado, `404` discusión/comentario/padre no existe, `409` username duplicado, `422` profundidad excedida, `500` fallo de persistencia.

---

## Arquitectura

**Modular monolith** (`controller → service → repository → JSON`), sin base de datos, microservicios ni Docker (requisito).

```
Angular (Fase 2)
   ↓ HTTP + Authorization: Bearer <token>
Spring Boot 4.1.1 (Java 21)
   ├─ controller: AuthController, UserController, DiscussionController, CommentController
   ├─ service: AuthService, TokenService, UserService, DiscussionService, CommentService
   ├─ repository: JsonFileRepository<T,ID> + UserRepository, DiscussionRepository, CommentRepository, TokenRepository
   ├─ config: ForumProperties, JacksonConfig (Jackson 3), CorsConfig, SecurityConfig, TokenAuthenticationFilter, RestAuthenticationEntryPoint
   ├─ dto + exception/GlobalExceptionHandler
   └─ data/*.json
```

- `ForumProperties` (`forum.data-dir`, `default-max-reply-depth=3`, `cors-allowed-origins`).
- `JacksonConfig` desactiva `WRITE_DATES_AS_TIMESTAMPS`, activa `INDENT_OUTPUT` y tolera propiedades desconocidas (Jackson 3: `tools.jackson`).
- `CorsConfig` permite `http://localhost:4200` con credenciales.

---

## Persistencia y seguridad

**Persistencia:** `JsonFileRepository<T,ID>` genérico. Cada entidad en su archivo (`users.json`, `discussions.json`, `comments.json`, `tokens.json`). Escrituras con `ReentrantReadWriteLock` por archivo + archivo temporal y `Files.move(ATOMIC_MOVE, REPLACE_EXISTING)` con fallback. Lecturas tolerantes a archivo ausente/vacío. Los comentarios se guardan planos con `parentId`; el árbol se construye en lectura.

**Seguridad:** Passwords con `BCryptPasswordEncoder` (nunca exponer `passwordHash`). Auth con **token opaco** UUID (`Authorization: Bearer`) persistido en `tokens.json`; `TokenAuthenticationFilter` (`OncePerRequestFilter`) resuelve el token y fija `User` como `principal` (`@AuthenticationPrincipal`). `SecurityFilterChain` stateless, CSRF off, CORS on. Entry point/denied handler devuelven el JSON uniforme de `ApiErrorResponse`.

---

## Pruebas

`./mvnw test` usa `target/test-data` (perfil `test`) y un `JsonMapper` aislado para repositorios.

| Test | Qué cubre |
|------|-----------|
| `ForumApiApplicationTests` | Contexto arranca y excluye `UserDetailsServiceAutoConfiguration` |
| `JsonFileRepositoryTest` (9) | Atomicidad, ReentrantReadWriteLock concurrente (8×10), archivo vacío, visibilidad entre instancias, fechas ISO |
| `UpdateUserSettingsRequestTest` (3) | Jackson 3: `absent`→no cambia, `null`→ilimitado, valor→`Optional.of` |
| `AuthServiceTest` (7) | BCrypt, duplicado, login válido/inválido, logout |
| `UserServiceTest` (6) | `getById`, 3 semánticas de settings, negativo |
| `DiscussionServiceTest` (5) | Crear, orden desc + `commentCount`, `findById`, propias |
| `CommentServiceTest` (8) | Niveles 1/2/3 con `maxDepth=3`, 4→422, `null` ilimitado, padre otra discusión/inexistente, discusión inexistente, árbol |
| `ForumApiIntegrationTest` (3) | `@SpringBootTest` + `MockMvc`: flujo E2E (register→login→discussion→comment→reply hasta 3→422→PATCH ilimitado→nivel 4/5→árbol), duplicado→409, login erróneo→401 |

---

## Estructura del proyecto

```
forum.test/
├── README.md
├── .gitignore
├── api/
│   ├── AGENTS.md
│   ├── pom.xml
│   ├── mvnw / .mvn/wrapper/
│   ├── src/main/java/com/forum/api/{config,model,dto,repository,service,controller,exception}
│   ├── src/main/resources/application.yml
│   ├── src/test/resources/application-test.yml
│   ├── src/test/java/com/forum/api/**Test.java
│   └── data/.gitkeep
├── app/
│   └── AGENTS.md
├── bruno/forum-api/    # colección Bruno (10 requests + local env)
└── docs/
    ├── prompts/02-api-design.md
    └── plans/phase-1-api-plan.md
```

Historial de commits en **Conventional Commits**; cada commit y push requirió aprobación explícita del usuario.

---

## Uso de IA

**Herramientas:** Muse Spark (OpenCode) como asistente principal. La evaluación permitía Copilot/ChatGPT/Claude/Gemini/Cursor, etc.; aquí se delegó el scaffolding y la generación inicial a la IA y se refinó manualmente.

**Prompts principales:**

- *“Diseño del API REST”* (`docs/prompts/02-api-design.md`): contrato completo (entidades, DTOs, endpoints, validaciones, códigos HTTP, persistencia, profundidad, estructura de paquetes y flujos) antes de escribir código.
- *“Genera starter Spring Boot desde start.spring.io con web/validation/security/lombok/configuration-processor/devtools”* y adaptación a Jackson 3.
- *“Implementa JsonFileRepository genérico con lock + escritura atómica y repository concretos”* y sus pruebas.
- *“En Jackson 3 `WRITE_DATES_AS_TIMESTAMPS` es `DateTimeFeature`, no `SerializationFeature`; `JsonMapper` es el mapper; `jackson-databind-nullable` no es compatible”* — corrección del modelo sugerido por la IA.

**Refinamiento:** El plan original proponía `jackson-databind-nullable` (`JsonNullable`) para `PATCH` `null` vs ausente. Al detectar el cambio a Jackson 3 (Boot 4.1.1 usa `tools.jackson`), se sustituyó por una **clase mutable con `Optional<Integer>`** y se verificó empíricamente (`OptionalProbeTest`): en un record ambos casos son `Optional.empty`; en una clase con setter `absent→null`, `null→Optional.empty`. Se documentó con `UpdateUserSettingsRequestTest` y se mantuvo el helper `isProvided()/isUnlimited()`. También se fijó `MAVEN_USER_HOME` local porque `~/.m2` pertenecía a `root`.

**Aprendizajes:**

- Boot 4 migra a Jackson 3 (`tools.jackson`), incompatibilizando librerías de Jackson 2; la IA a veces propone dependencias obsoletas.
- La API de `JsonMapperBuilderCustomizer` y `DateTimeFeature` en Boot 4 difiere de Boot 3.
- `AutoConfigureMockMvc` cambió de paquete (`org.springframework.boot.webmvc.test.autoconfigure`).
- Con `@TempDir` y `JsonMapper.builder()` se prueban repositorios sin Spring; con `MockMvc` + `target/test-data` se prueba el flujo real.

---

## Explicación de la solución

**Componentes:** `User` (`maxReplyDepth`), `Discussion` (`authorId`), `Comment` (`discussionId`, `parentId` nullable), `AuthToken`. Relaciones por `UUID` (`id`, no `_id`).

**Flujo Front ↔ Back:** Angular → `POST /api/auth/register` → BCrypt → `users.json` → `POST /api/auth/login` → token opaco en `tokens.json` → `Authorization: Bearer` en cada request → `TokenAuthenticationFilter` → `SecurityContext` → controllers → services → repositories.

**Manejo de datos:** DTOs (`RegisterRequest`, `CreateDiscussionRequest`, `CreateCommentRequest`, `UpdateUserSettingsRequest`, `UserResponse`, `DiscussionSummary/Response`, `CommentResponse` con `replies`) separan modelo interno de HTTP; `passwordHash` nunca se expone.

**Creación de comentarios:** Ver `CommentService.create` — valida discusión existente, `parentId` existente y de la misma discusión, resuelve dueño → `maxReplyDepth`, calcula nivel caminando `parentId` con detección de ciclos, rechaza si excede, genera `UUID` y guarda.

**Almacenamiento:** Plano en `comments.json` (`parentId` nullable). **Renderizado:** `CommentService.buildTree` agrupa por `parentId`, ordena por `createdAt` y recursa: `GET /api/discussions/{id}` devuelve `DiscussionResponse { comments: [ {replies:[...]} ] }`.

**Relaciones padre-hijo:** `parentId=null` = hija directa de la discusión; `parentId=UUID` = reply; el nivel se computa como `discusión=0, direct=1, reply=2…`.

---

## Mejoras identificadas

**1) Concurrencia y atomicidad de la persistencia en JSON**

- *Problema:* Escrituras simultáneas corrompen el archivo; lectura/escritura sin lock es frágil.
- *Riesgo:* Pérdida de datos y respuestas 500 bajo concurrencia (dos usuarios comentando a la vez).
- *Solución implementada:* `ReentrantReadWriteLock` por archivo + escritura a archivo temporal y `Files.move(ATOMIC_MOVE)` con fallback; lectura tolerante a archivo ausente/vacío y directorios `createDirectories`. Ver `JsonFileRepository` y `JsonFileRepositoryTest.concurrentSavesKeepFileConsistent`.
- *Beneficio:* Integridad de los `*.json` con costo mínimo; tests lo verifican.

**2) Manejo de errores y validaciones**

- *Problema:* El scaffold solo devolvía 500 por defecto; sin detalles de campo y sin mapear 401/403/409/422, difícil de consumir desde Angular.
- *Riesgo:* Frontend no distingue “username duplicado” de “JSON malformado” de “profundidad excedida”.
- *Solución implementada:* DTOs con Bean Validation (`@NotBlank`, `@Size`), estructura uniforme `{timestamp,status,error,message,path,details[]}` en `GlobalExceptionHandler` para `MethodArgumentNotValidException`, `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException`, `ApiException` (409/404/422…), `AccessDenied/Authentication`, y `UncheckedIOException/JacksonException`; `CORS` configurado y `RestAuthenticationEntryPoint`.
- *Beneficio:* Contratos predecibles; el cliente distingue 409 vs 422 vs 400 y muestra mensajes por campo.

*Mejoras pendientes (no implementadas, como evolución):* paginación en `GET /api/discussions`, índice en memoria por `discussionId`, y migrar `tokens.json` a JWT/stateless si escala.

---

## Cambio funcional — niveles configurables

La versión inicial sugería un límite fijo. Se modificó para que **cada dueño de discusión controle su propio límite** y pueda cambiarlo en caliente.

- **Modelo:** `User.maxReplyDepth: Integer|null` (default `3`; `null`=ilimitado) con `ForumProperties.defaultMaxReplyDepth`.
- **Contrato:** `GET /api/users/me/settings` y `PATCH /api/users/me/settings` con `{maxReplyDepth: number|null}`. El reto es distinguir `null` (ilimitado) de campo ausente (no cambiar). Se resolvió con una **clase mutable** `UpdateUserSettingsRequest { Optional<Integer> maxReplyDepth }` — `absent→null`, `null→Optional.empty`, `valor→Optional.of` — documentado con `UpdateUserSettingsRequestTest`. Alternativa considerada `PUT` con reemplazo total; se mantuvo `PATCH` por ser más expresivo.
- **Regla aplicada:** `discusión=0, comentario direct=1`. Al crear un comentario, `CommentService.computeNewLevel(parentId)` camina la cadena con guardas anti-ciclo (visited set, límite 1000) y rechaza con `422 Maximum reply depth exceeded` si `maxDepth!=null && newLevel>maxDepth`. Comentarios ya creados se conservan (grandfathering) si el dueño baja el límite.
- **Implicaciones:** Respuesta de `GET /api/discussions/{id}` pasa de lista plana a árbol recursivo construido en el servicio; `POST /api/discussions/{id}/comments` valida dinámicamente según la configuración actual del dueño; la UI puede mostrar/ocultar el composer según el nivel.

Ejemplo:

```
# propietario con maxReplyDepth=3
POST /api/discussions/{id}/comments {content:"A"}                     → nivel 1 OK
POST ... {content:"B", parentId: A}                                  → nivel 2 OK
POST ... {content:"C", parentId: B}                                  → nivel 3 OK
POST ... {content:"D", parentId: C}                                  → 422

PATCH /api/users/me/settings {maxReplyDepth:5}
POST ... {content:"D", parentId: C}                                  → nivel 4 OK
PATCH ... {maxReplyDepth:null}
POST ... {content:"E", parentId: D}                                  → nivel 5 OK (ilimitado)
```

---

## Retos encontrados

**Máquina sin Java/Maven y `~/.m2` de `root`:** `java` y `mvn` no estaban instalados y `~/.m2` pertenecía a `root`, por lo que `mvnw` fallaba con `mkdir: Permission denied`. Se instaló Temurin 21 en `~/.local/opt/jdk-21` sin `sudo` (vía Adoptium tarball) y se configuró `JAVA_HOME/PATH` y `MAVEN_USER_HOME=~/.local/share/maven` + `MAVEN_OPTS=-Dmaven.repo.local=...`. La solución evitó `sudo` y es reproducible.

**Spring Boot 4 + Jackson 3:** El starter por defecto trae `JsonMapper` (`tools.jackson`) y `DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS` en lugar de `SerializationFeature`. La IA propuso `jackson-databind-nullable` (Jackson 2) y `AutoConfigureMockMvc` en su paquete antiguo. Se adaptó a `JacksonConfig` con `JsonMapperBuilderCustomizer` y se migraron imports/tests, verificando con `dependency:tree` y probes de deserialización.

**Pérdida temporal de modo plan:** Dos commits requirieron re-ingresar a modo construcción; el historial se mantuvo lineal.

Verificación final: `./mvnw test` **42/42** y colección Bruno bajo `bruno/forum-api/` lista para ejercitar el flujo real.
