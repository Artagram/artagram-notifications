package com.artagram.notifications.events;

import com.artagram.notifications.notificacion.NotificacionDocument;
import com.artagram.notifications.notificacion.NotificacionSseService;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Portado de la logica de construccion de notificaciones de ChatEventListener
 * (artagram-chat) — la logica de mensajes de sala/directorio de usuarios se
 * queda en chat, no es parte de este servicio.
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificacionSseService sseService;

    public NotificationEventListener(NotificacionSseService sseService) {
        this.sseService = sseService;
    }

    @RabbitListener(queues = NotificationsEventsRabbitMqConfig.QUEUE_FOLLOW_CREATED)
    public void onFollowCreated(Map<String, Object> payload) {
        try {
            UUID seguidoId = UUID.fromString(String.valueOf(payload.get("seguidoId")));
            UUID seguidorId = UUID.fromString(String.valueOf(payload.get("seguidorId")));
            String seguidorNombre = String.valueOf(payload.getOrDefault("seguidorNombre", "Un artista"));

            sseService.emitir(new NotificacionDocument(
                    UUID.randomUUID(), seguidoId, "NUEVO_SEGUIDOR", seguidorId,
                    seguidorNombre, seguidorId.toString(), "@" + seguidorNombre + " comenzó a seguirte"
            ));
        } catch (Exception e) {
            log.error("Error consumiendo follow.created: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = NotificationsEventsRabbitMqConfig.QUEUE_LIKE_CREATED)
    public void onLikeCreated(Map<String, Object> payload) {
        try {
            UUID autorId = UUID.fromString(String.valueOf(payload.get("autorId")));
            UUID usuarioId = UUID.fromString(String.valueOf(payload.get("usuarioId")));
            String usuarioNombre = String.valueOf(payload.getOrDefault("usuarioNombre", "Un artista"));
            String publicacionId = String.valueOf(payload.get("publicacionId"));

            if (!autorId.equals(usuarioId)) {
                sseService.emitir(new NotificacionDocument(
                        UUID.randomUUID(), autorId, "NUEVO_LIKE", usuarioId,
                        usuarioNombre, publicacionId, "@" + usuarioNombre + " le dio me gusta a tu obra"
                ));
            }
        } catch (Exception e) {
            log.error("Error consumiendo like.created: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = NotificationsEventsRabbitMqConfig.QUEUE_COMMENT_CREATED)
    public void onCommentCreated(Map<String, Object> payload) {
        try {
            UUID autorPostId = UUID.fromString(String.valueOf(payload.get("publicacionAutorId")));
            UUID autorComentarioId = UUID.fromString(String.valueOf(payload.get("autorId")));
            String autorNombre = String.valueOf(payload.getOrDefault("autorNombre", "Un artista"));
            String publicacionId = String.valueOf(payload.get("publicacionId"));
            String contenido = String.valueOf(payload.getOrDefault("contenido", ""));

            if (!autorPostId.equals(autorComentarioId)) {
                sseService.emitir(new NotificacionDocument(
                        UUID.randomUUID(), autorPostId, "NUEVO_COMENTARIO", autorComentarioId,
                        autorNombre, publicacionId, "@" + autorNombre + " comentó: " + contenido
                ));
            }
        } catch (Exception e) {
            log.error("Error consumiendo comment.created: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = NotificationsEventsRabbitMqConfig.QUEUE_SUBSCRIPTION_UPDATED)
    public void onSubscriptionUpdated(Map<String, Object> payload) {
        try {
            UUID usuarioId = UUID.fromString(String.valueOf(payload.get("usuarioId")));
            boolean premium = Boolean.TRUE.equals(payload.get("premium"));
            sseService.emitir(new NotificacionDocument(
                    UUID.randomUUID(), usuarioId, premium ? "SUSCRIPCION_PREMIUM" : "SUSCRIPCION_VENCIDA", null,
                    "ArtaGram", null,
                    premium ? "¡Tu plan PRO está activo!" : "Tu plan PRO venció. Tu cuenta volvió al plan gratuito."
            ));
        } catch (Exception e) {
            log.error("Error consumiendo subscription.updated: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = NotificationsEventsRabbitMqConfig.QUEUE_AI_COMPLETED)
    public void onAiCompleted(Map<String, Object> payload) {
        try {
            // En este evento autorId es quien pidio el analisis, no siempre el autor de la obra.
            UUID solicitanteId = UUID.fromString(String.valueOf(payload.get("autorId")));
            String publicacionId = String.valueOf(payload.get("publicacionId"));
            sseService.emitir(new NotificacionDocument(
                    UUID.randomUUID(), solicitanteId, "ANALISIS_IA_LISTO", null,
                    "ArtaGram AI", publicacionId, "El análisis técnico de tu obra ya está disponible"
            ));
        } catch (Exception e) {
            log.error("Error consumiendo ai.completed: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = NotificationsEventsRabbitMqConfig.QUEUE_POST_MODERATED)
    public void onPostModerated(Map<String, Object> payload) {
        try {
            UUID autorId = UUID.fromString(String.valueOf(payload.get("autorId")));
            String publicacionId = String.valueOf(payload.get("publicacionId"));
            String titulo = String.valueOf(payload.getOrDefault("titulo", "tu obra"));
            String mensaje = "RESTAURADA".equals(payload.get("accion"))
                    ? "Revisamos \"" + titulo + "\" y ya está visible de nuevo"
                    : "Ocultamos \"" + titulo + "\" por reportes de la comunidad mientras se revisa";
            sseService.emitir(new NotificacionDocument(
                    UUID.randomUUID(), autorId, "OBRA_MODERADA", null, "ArtaGram", publicacionId, mensaje
            ));
        } catch (Exception e) {
            log.error("Error consumiendo post.moderated: {}", e.getMessage());
        }
    }

    /**
     * Portado de ChatPersonalService (artagram-chat): antes emitia esta
     * notificacion con un SSE propio de chat, inalcanzable para el cliente
     * porque el gateway ya enruta /api/notificaciones/** solo hacia aqui.
     */
    @RabbitListener(queues = NotificationsEventsRabbitMqConfig.QUEUE_PERSONAL_MESSAGE_NOTIFICATION)
    public void onPersonalMessageNotification(Map<String, Object> payload) {
        try {
            UUID destinatarioId = UUID.fromString(String.valueOf(payload.get("destinatarioId")));
            UUID actorId = UUID.fromString(String.valueOf(payload.get("actorId")));
            String actorNombre = String.valueOf(payload.getOrDefault("actorNombre", "Un artista"));
            String referenciaId = String.valueOf(payload.get("referenciaId"));
            String contenido = String.valueOf(payload.getOrDefault("contenido", ""));

            sseService.emitir(new NotificacionDocument(
                    UUID.randomUUID(), destinatarioId, NotificacionDocument.TIPO_MENSAJE, actorId,
                    actorNombre, referenciaId, "Te envió un mensaje: " + contenido
            ));
        } catch (Exception e) {
            log.error("Error consumiendo personal.message.notification: {}", e.getMessage());
        }
    }
}
