package com.artagram.notifications.notificacion.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificacionDto(
        UUID id,
        UUID destinatarioId,
        String tipo,
        UUID actorId,
        String actorNombre,
        String referenciaId,
        String mensaje,
        boolean leida,
        Instant creadoEn
) {
}
