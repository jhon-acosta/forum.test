# Prompt 02 — Diseño del API REST

> Prompt utilizado para diseñar el contrato del API antes de implementar el backend.
> Se conserva como registro del proceso asistido por IA (Fase 1).

Actúa como Senior Software Engineer especializado en Spring Boot y diseño de APIs REST.

Estamos desarrollando una aplicación tipo foro/discussion board para una prueba técnica Full Stack.

## Contexto técnico

La aplicación tendrá:

* Frontend: Angular
* Backend: Spring Boot
* Persistencia: archivos JSON
* Versionado: Git
* No utilizar base de datos.
* No utilizar MongoDB.
* No utilizar microservicios.
* No utilizar Docker.
* No implementar OAuth/JWT en esta primera versión.
* La arquitectura debe mantenerse como un modular monolith sencillo y mantenible.

La comunicación será:

```text
Angular
   ↓ HTTP/REST
Spring Boot
   ↓
Controller
   ↓
Service
   ↓
Repository
   ↓
JSON files
```

## Entidades principales

### User

Campos: `id: UUID`, `username: String`, `passwordHash: String`, `maxReplyDepth: Integer | null`, `createdAt: LocalDateTime`.

Reglas:

* `id` generado automáticamente con UUID.
* Utilizar `id`, no `_id`. No utilizar ObjectId.
* `username` único.
* El registro solo solicita `username` y `password`.
* La contraseña nunca se almacena directamente; usar BCrypt para `passwordHash`.
* `maxReplyDepth` por defecto `3`. `null` representa profundidad ilimitada.
* El usuario autenticado puede modificar su propia configuración de `maxReplyDepth`.

### Discussion

Campos: `id: UUID`, `title: String`, `content: String`, `authorId: UUID`, `createdAt: LocalDateTime`.

Reglas:

* `id` generado con UUID.
* `authorId` referencia al usuario propietario.
* Discusiones públicas para usuarios autenticados.
* Un usuario puede crear discusiones y consultar las suyas.

### Comment

Campos: `id: UUID`, `discussionId: UUID`, `parentId: UUID | null`, `authorId: UUID`, `content: String`, `createdAt: LocalDateTime`.

Reglas:

* `id` generado con UUID.
* `discussionId` identifica la discusión.
* `parentId = null` = respuesta directa a la discusión.
* Comentarios planos en `comments.json`; jerarquía construida en la capa de servicio.
* La respuesta de la API representa comentarios anidados recursivamente.

## Regla de profundidad

`maxReplyDepth` pertenece al propietario de la discusión.

```text
Discusión = nivel 0
Comentario directo = nivel 1
Respuesta a comentario = nivel 2
Respuesta siguiente = nivel 3
```

Con `maxReplyDepth = 3` se permiten comentarios hasta nivel 3 y se rechaza nivel 4.
Con `maxReplyDepth = null` la profundidad es ilimitada.

## Persistencia

```text
data/
├── users.json
├── discussions.json
└── comments.json
```

No mezclar entidades en un único archivo. Referencias por UUID.

---

# Objetivo de esta tarea

Antes de escribir código, diseña el contrato completo de la API REST.

## 1. Endpoints de autenticación

Registrar usuario, iniciar sesión (y cerrar sesión si aplica con la estrategia elegida).
Para cada endpoint: método HTTP, URL, propósito, request/response body, códigos HTTP y validaciones.

## 2. Endpoints de usuario

Consultar usuario autenticado, consultar/configurar `maxReplyDepth`, actualizar configuración.
Preferir `/api/users/me` y `/api/users/me/settings`.

## 3. Endpoints de discusiones

Listar, obtener, crear y obtener las propias. Evaluar:

```text
GET /api/discussions
GET /api/discussions/{id}
GET /api/users/me/discussions
POST /api/discussions
```

## 4. Endpoints de comentarios

Obtener comentarios de una discusión, crear comentario principal y responder. Validar `parentId`,
`discussionId` y `maxReplyDepth`. Preferir:

```text
POST /api/discussions/{discussionId}/comments
{ "content": "Mi comentario", "parentId": null }
```

## 5. DTOs

`RegisterRequest`, `LoginRequest`, `CreateDiscussionRequest`, `CreateCommentRequest`,
`UpdateUserSettingsRequest` y los DTOs de respuesta. `passwordHash` nunca debe exponerse.

## 6. Respuesta jerárquica

La API debe entregar la discusión con `comments` anidados recursivamente (`replies`),
mientras `comments.json` permanece plano con `parentId`.

## 7. Validaciones y errores

username obligatorio/único, password obligatorio, title/content/comentario obligatorios,
usuario autenticado, discusión/comentario padre existentes, padre de la misma discusión,
profundidad máxima, propietario de la discusión, usuario modificando solo su configuración.
Estructura de error consistente, por ejemplo `{ "status": 400, "message": "..." }`.

## 8. Códigos HTTP

200, 201, 204, 400, 401, 403, 404, 409, 500 (usar solo los que correspondan).

## 9. Autenticación

Sin JWT/OAuth en esta primera versión. Proponer una estrategia sencilla y evolucionable.

## 10. Estructura final del backend

Paquetes `controller/`, `service/`, `repository/`, `model/`, `dto/`, `config/`, `exception/`.

## 11. Flujo de las operaciones principales

Registro, crear discusión y crear respuesta (identificar discusión, padre, dueño,
`maxReplyDepth`, calcular nivel, validar, generar UUID y guardar).

## 12. Decisiones de diseño

Sección `## Decisiones tomadas` con UUID vs ObjectId, `id` vs `_id`, JSON separado por entidad,
comentarios planos + `parentId`, árbol al responder, `maxReplyDepth` del dueño,
`null` ilimitado, BCrypt, DTOs y modular monolith.

## Restricciones

No implementes código todavía. No crees archivos. No instales dependencias.
Primero quiero revisar y aprobar el contrato de la API.
