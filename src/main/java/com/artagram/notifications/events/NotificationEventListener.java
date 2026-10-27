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
            String postId = String.valueOf(payload.get("postId"));

            if (!autorId.equals(usuarioId)) {
                sseService.emitir(new NotificacionDocument(
                        UUID.randomUUID(), autorId, "NUEVO_LIKE", usuarioId,
                        usuarioNombre, postId, "@" + usuarioNombre + " le dio me gusta a tu obra"
                ));
            }
        } catch (Exception e) {
            log.error("Error consumiendo like.created: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = NotificationsEventsRabbitMqConfig.QUEUE_COMMENT_CREATED)
    public void onCommentCreated(Map<String, Object> payload) {
        try {
            UUID autorPostId = UUID.fromString(String.valueOf(payload.get("autorPostId")));
            UUID autorComentarioId = UUID.fromString(String.valueOf(payload.get("autorId")));
            String autorNombre = String.valueOf(payload.getOrDefault("autorNombre", "Un artista"));
            String postId = String.valueOf(payload.get("postId"));
            String contenido = String.valueOf(payload.getOrDefault("contenido", ""));

            if (!autorPostId.equals(autorComentarioId)) {
                sseService.emitir(new NotificacionDocument(
                        UUID.randomUUID(), autorPostId, "NUEVO_COMENTARIO", autorComentarioId,
                        autorNombre, postId, "@" + autorNombre + " comentó: " + contenido
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
            if (premium) {
                sseService.emitir(new NotificacionDocument(
                        UUID.randomUUID(), usuarioId, "SUSCRIPCION_PREMIUM", null,
                        "ArtaGram", null, "¡Tu suscripción Premium está activa! Disfruta de historial ilimitado."
                ));
            }
        } catch (Exception e) {
            log.error("Error consumiendo subscription.updated: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = NotificationsEventsRabbitMqConfig.QUEUE_AI_COMPLETED)
    public void onAiCompleted(Map<String, Object> payload) {
        try {
            UUID solicitanteId = UUID.fromString(String.valueOf(payload.get("solicitanteId")));
            String postId = payload.get("postId") != null ? String.valueOf(payload.get("postId")) : null;
            sseService.emitir(new NotificacionDocument(
                    UUID.randomUUID(), solicitanteId, "ANALISIS_IA_LISTO", null,
                    "ArtaGram AI", postId, "El análisis técnico de tu obra ya está disponible"
            ));
        } catch (Exception e) {
            log.error("Error consumiendo ai.completed: {}", e.getMessage());
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
                    UUID.randomUUID(), destinatarioId, "MENSAJE_DIRECTO", actorId,
                    actorNombre, referenciaId, "Te envió un mensaje: " + contenido
            ));
        } catch (Exception e) {
            log.error("Error consumiendo personal.message.notification: {}", e.getMessage());
        }
    }
}
