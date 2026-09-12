# AGENTS.md — app (Angular)

Guía para agentes de IA y desarrolladores que trabajen en el frontend.

## Stack (Fase 2)

- Angular 18+ (standalone, TypeScript)
- Angular CLI (`npx ng` o `npm i -g @angular/cli`)
- Estado de sesión con token opaco en `localStorage` + interceptor HTTP que añade `Authorization: Bearer`.

## Comandos

```bash
npx @angular/cli new app --routing --style=css   # scaffold (pendiente)
npm install
npm start        # ng serve -> http://localhost:4200 (proxy a :8080)
npm test         # Karma/Jest
npm run build
```

## Estructura prevista

```
app/src/app/
├── core/           # auth.service, token.interceptor, guards
├── features/
│   ├── auth/       # login, register
│   ├── discussions/# list, detail, create
│   └── comments/   # comment-tree (recursivo), composer
└── shared/
```

## Integración con la API

Base `http://localhost:8080` (ver `bruno/` para ejemplos).
CORS ya configurado en el backend para `http://localhost:4200`.

## Convenciones

- Commits en **Conventional Commits**.
- No agregar comentarios al código salvo que se soliciten.
- Commits y push requieren aprobación explícita del usuario.
