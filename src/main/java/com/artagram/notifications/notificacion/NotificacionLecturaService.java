package com.artagram.notifications.notificacion;

import com.artagram.notifications.notificacion.dto.NoLeidasDto;
import java.util.UUID;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
public class NotificacionLecturaService {

    private final NotificacionRepository repository;
    private final MongoTemplate mongoTemplate;

    public NotificacionLecturaService(NotificacionRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    public NoLeidasDto contarNoLeidas(UUID usuarioId) {
        String mensaje = NotificacionDocument.TIPO_MENSAJE;
        return new NoLeidasDto(
                repository.countByDestinatarioIdAndLeidaFalseAndTipoNot(usuarioId, mensaje),
                repository.countByDestinatarioIdAndLeidaFalseAndTipo(usuarioId, mensaje));
    }

    public void marcarTodas(UUID usuarioId, boolean mensajes) {
        Criteria tipo = Criteria.where("tipo");
        Criteria criterio = Criteria.where("destinatarioId").is(usuarioId).and("leida").is(false)
                .andOperator(mensajes ? tipo.is(NotificacionDocument.TIPO_MENSAJE) : tipo.ne(NotificacionDocument.TIPO_MENSAJE));
        mongoTemplate.updateMulti(new Query(criterio), new Update().set("leida", true), NotificacionDocument.class);
    }
}
