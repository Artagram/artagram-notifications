package com.artagram.notifications.events;

import com.artagram.notifications.email.EmailService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * artagram-auth sigue generando el token/enlace de verificacion (y decidiendo
 * si la verificacion es requerida) — publica el evento ya con el enlace
 * armado, este listener solo lo entrega por el canal de email configurado.
 */
@Component
public class VerificationEmailEventListener {

    private static final Logger log = LoggerFactory.getLogger(VerificationEmailEventListener.class);

    private final EmailService emailService;

    public VerificationEmailEventListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = NotificationsEventsRabbitMqConfig.QUEUE_VERIFICATION_REQUESTED)
    public void onVerificationRequested(Map<String, Object> payload) {
        try {
            String correo = String.valueOf(payload.get("correo"));
            String nombreArtista = String.valueOf(payload.getOrDefault("nombreArtista", "artista"));
            String enlaceVerificacion = String.valueOf(payload.get("enlaceVerificacion"));
            emailService.enviarVerificacion(correo, nombreArtista, enlaceVerificacion);
        } catch (Exception e) {
            log.error("Error consumiendo user.verification.requested: {}", e.getMessage());
        }
    }
}
