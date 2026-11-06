package com.artagram.notifications.notificacion;

import java.util.List;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NotificacionRepository extends MongoRepository<NotificacionDocument, UUID> {

    List<NotificacionDocument> findTop50ByDestinatarioIdAndTipoNotOrderByCreadoEnDesc(UUID destinatarioId, String tipo);

    long countByDestinatarioIdAndLeidaFalseAndTipoNot(UUID destinatarioId, String tipo);

    long countByDestinatarioIdAndLeidaFalseAndTipo(UUID destinatarioId, String tipo);
}
