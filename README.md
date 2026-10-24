# artagram-notifications

Microservicio de notificaciones de Artagram: centro de notificaciones in-app
(persistido en Mongo + entregado en vivo por SSE) y envío de email. Consolida
dos responsabilidades que antes vivían repartidas en otros servicios y que no
les pertenecían:

- **`artagram-chat`** tenía `NotificacionSseService`, `NotificacionDocument` y
  `ChatEventListener` (la parte de notificaciones, no la de mensajería de
  sala — eso se queda en chat).
- **`artagram-auth`** tenía `EmailService`/`LogEmailService`/`SmtpEmailService`
  y llamaba a `enviarVerificacion(...)` directamente desde `AuthService` al
  registrar un usuario.

Ambas piezas quedan como código muerto en sus repos originales (no se borran
en esta migración — decomiso es un paso posterior) y el camino nuevo pasa por
aquí.

## Qué hace

- `GET /api/notificaciones` — últimas 50 notificaciones del usuario autenticado.
- `POST /api/notificaciones/{id}/leer` — marcar una como leída.
- `GET /api/notificaciones/stream` (SSE) — stream en vivo + heartbeat cada 25s.
- Consume de RabbitMQ: `follow.created`, `like.created`, `comment.created`,
  `subscription.updated` (solo si `premium=true`), `ai.completed` → crea un
  `NotificacionDocument` y lo empuja por SSE si el usuario está conectado.
- Consume `user.verification.requested` (nuevo, publicado por `artagram-auth`)
  → envía el email de verificación (modo `log` o `smtp` según `EMAIL_MODE`).

## Por qué un vhost propio en RabbitMQ

Decisión explícita: aislar el tráfico de notificaciones del resto de
servicios sin pagar/operar una instancia de broker separada. Este servicio
se conecta al vhost `notifications` (no al `/` que usan auth/users/feed/chat/
canvas/ai-worker/media-worker). Un vhost es una partición dura — el exchange
`artagram.events` que declara `NotificationsEventsRabbitMqConfig` es una
copia *local* a ese vhost, no el mismo objeto que el de `/`. El puente real
(reenviar todo lo publicado en `/` hacia este vhost) es un **Shovel** de
RabbitMQ configurado en `artagram-infra/rabbitmq/definitions.json` — no hay
nada en el código de este servicio que dependa de eso; desde su punto de
vista simplemente hay mensajes en sus colas `notifications.*`.

## Limitación conocida (SSE en memoria)

`NotificacionSseService` guarda los `SseEmitter` en un mapa en memoria de la
instancia — igual que en `artagram-chat` hoy. Si este servicio escala a más
de una réplica, un emitter conectado a la instancia A no recibe lo que se
emite desde la instancia B. El modelo correcto para eso es el que ya usa
`artagram-canvas` (`RealtimeBroadcaster`, relay vía Redis Pub/Sub) — no se
introduce esa dependencia aquí sin una necesidad concreta de escalar
horizontalmente.

## Variables de entorno

Ver `.env.example`. Las relevantes:

| Variable | Default | Nota |
|---|---|---|
| `SPRING_DATA_MONGODB_URI` | `mongodb://...db_notificaciones` | DB creada de forma perezosa por Mongo |
| `RABBITMQ_VHOST` | `notifications` | único servicio que no usa `/` |
| `RABBITMQ_USER`/`RABBITMQ_PASSWORD` | `notifications`/`notifications_secret` | credenciales scoped solo a ese vhost |
| `JWT_PUBLIC_KEY_PATH` | `../artagram-infra/secrets-dev/jwt-public.pem` | misma llave que el resto de servicios |
| `EMAIL_MODE` | `log` | `log` (solo loguea) o `smtp` |

## Cómo correrlo en local

```
docker compose -f ../artagram-infra/compose.yml -f ../artagram-infra/compose.services.yml \
  up -d --build artagram-notifications
curl -s http://localhost:8090/actuator/health
```

Requiere que `artagram-infra` tenga provisionado el vhost `notifications` y
el shovel que lo puentea con `/` (ver su propio README/definitions.json).
