# Plan — Fase 1: API (Spring Boot)

Registro del plan aprobado para construir el backend del foro.

## Decisiones acordadas

| Tema | Decisión |
|---|---|
| JDK | Java 21 Temurin instalado en `~/.local/opt/jdk-21` (sin `sudo`) |
| Framework | Spring Boot 4.1.1 (Maven) |
| Auth | Token opaco (UUID) en `Authorization: Bearer`, persistido en `tokens.json` |
| Profundidad ilimitada | `null`; distinguir `null` de campo ausente en el PATCH |
| Repositorio | Monorepo `forum.test/` con `api/`, `app/`, `docs/` |
| Commits | Conventional Commits; cada commit y push requiere aprobación del usuario |
| Docs | `docs/prompts/` y `docs/plans/` se suben en el commit final |

## Hallazgos del entorno

- `java`, `mvn`, `gradle` no estaban instalados.
- `~/.m2` pertenece a `root`; se usa `MAVEN_USER_HOME=~/.local/share/maven` y
  `MAVEN_OPTS=-Dmaven.repo.local=.../repository`.
- Spring Boot 4.1.1 usa **Jackson 3** (`tools.jackson.core`), incompatible con
  `jackson-databind-nullable` (Jackson 2). El PATCH de settings se resolverá con `Optional<Integer>`.

## Estructura

```
forum.test/
├── README.md
├── .gitignore
├── AGENTS.md (opcional)
├── api/
│   ├── AGENTS.md
│   └── src/{main,test}/java/com/forum/api/{config,model,dto,repository,service,controller,exception}
├── app/            (Fase 2)
└── docs/{prompts,plans}
```

## Pasos y commits

| # | Paso | Commit |
|---|---|---|
| 0 | JDK + extensiones VS Code + git init/remote/checkout `main` | (sin commit) |
| 1 | Scaffold `api/`, `.gitignore`, `api/AGENTS.md` | `chore: scaffold Spring Boot API project` |
| 2 | `application.yml`, perfil test, `ForumProperties`, ObjectMapper, CORS | `chore(config): add application config and profiles` |
| 3 | Modelos `User`, `Discussion`, `Comment`, `AuthToken` | `feat(domain): add core domain models` |
| 4 | `JsonFileRepository` (lock + escritura atómica) + repos + test | `feat(persistence): add atomic JSON file repositories` |
| 5 | DTOs, validación, `ApiException`, `GlobalExceptionHandler` | `feat(api): add DTOs, validation and global error handling` |
| 6 | BCrypt, `TokenStore`, filtro Bearer, `SecurityConfig`, Auth + test | `feat(auth): add opaque token authentication` |
| 7 | Usuarios `/me`, `/me/settings` + test | `feat(user): add profile and settings endpoints` |
| 8 | Discusiones list/get/create/mine + test | `feat(discussion): add discussion endpoints` |
| 9 | Comentarios + árbol + `maxReplyDepth` (3/5/null) + test | `feat(comment): add nested comments with configurable reply depth` |
| 10 | Integración E2E + seed + colección Bruno | `test(api): add end-to-end tests and seed data` |
| 11 | README + docs/prompts + docs/plans | `docs: add README, implementation plans and prompts` |

## Pruebas unitarias por paso

- 4: `JsonFileRepositoryTest` (atomicidad, concurrencia, archivos vacíos/ausentes).
- 6: `AuthServiceTest` (BCrypt, username duplicado, login válido/inválido).
- 7: `UserServiceTest` (`Optional` null/ausente, solo el propio usuario).
- 8: `DiscussionServiceTest` (crear/listar/obtener/mías).
- 9: `CommentServiceTest` (niveles, límite 3/5/null, padre ajeno, huérfano, ciclo, árbol).
- 10: integración `@SpringBootTest` + `MockMvc` (flujo E2E).
