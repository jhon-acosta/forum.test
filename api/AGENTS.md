# AGENTS.md — api (Spring Boot)

Guía para agentes de IA y desarrolladores que trabajen en el backend del foro.

## Stack

- Java 21 (Temurin), Spring Boot 4.1.1, Maven (Maven Wrapper `./mvnw`).
- Persistencia en archivos JSON (sin base de datos, sin microservicios, sin Docker).
- JSON con **Jackson 3** (`tools.jackson.core`). Ojo: `com.fasterxml.jackson` (Jackson 2) no aplica.
  Por eso `org.openapitools:jackson-databind-nullable` **no** es compatible.

## Entorno en esta máquina

`~/.m2` pertenece a `root`, así que se usa un Maven home de usuario. Antes de compilar:

```bash
export JAVA_HOME="$HOME/.local/opt/jdk-21"
export PATH="$JAVA_HOME/bin:$PATH"
export MAVEN_USER_HOME="$HOME/.local/share/maven"
export MAVEN_OPTS="-Dmaven.repo.local=$HOME/.local/share/maven/repository"
```

En VS Code basta con que el `settings.json` apunte a `java.jdt.ls.java.home` = `~/.local/opt/jdk-21`.

## Comandos

```bash
./mvnw test          # ejecuta las pruebas
./mvnw spring-boot:run   # arranca la API (http://localhost:8080)
./mvnw clean package # construye el jar
```

## Arquitectura (modular monolith)

```
controller -> service -> repository -> JSON files
```

- `config/` configuración (ObjectMapper, CORS, seguridad, propiedades).
- `model/` entidades de dominio.
- `dto/` contratos de entrada/salida HTTP (los modelos no se exponen directo).
- `repository/` acceso a `data/*.json`.
- `service/` lógica de negocio (incluye validación de profundidad y armado del árbol).
- `controller/` endpoints REST.
- `exception/` errores y `@RestControllerAdvice`.

## Reglas de negocio clave

- `id` = UUID generado por la app. Nunca `_id` ni ObjectId.
- Comentarios planos en `comments.json` con `parentId`; el árbol se arma en el service.
- `maxReplyDepth` pertenece al dueño de la discusión: discusión = nivel 0, comentario directo = nivel 1.
  Default `3`; `null` = ilimitado.
- Passwords con BCrypt; nunca exponer `passwordHash`.

## Convenciones

- Commits en **Conventional Commits** (`feat`, `fix`, `test`, `docs`, `chore`, `refactor`).
- No agregar comentarios al código salvo que se soliciten.
- Cada feature debe incluir sus pruebas unitarias (`*Test`).
- Commits y push requieren aprobación explícita del usuario.
