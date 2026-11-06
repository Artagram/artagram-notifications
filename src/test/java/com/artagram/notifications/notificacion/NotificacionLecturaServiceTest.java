package com.artagram.notifications.notificacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.artagram.notifications.notificacion.dto.NoLeidasDto;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;

class NotificacionLecturaServiceTest {

    private final NotificacionRepository repository = mock(NotificacionRepository.class);
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final NotificacionLecturaService service = new NotificacionLecturaService(repository, mongo);
    private final UUID usuarioId = UUID.randomUUID();

    private String filtroDe(boolean mensajes) {
        service.marcarTodas(usuarioId, mensajes);
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).updateMulti(query.capture(), any(UpdateDefinition.class), eq(NotificacionDocument.class));
        return query.getValue().getQueryObject().toJson();
    }

    @Test
    void cuentaAparteLosMensajes() {
        when(repository.countByDestinatarioIdAndLeidaFalseAndTipoNot(usuarioId, NotificacionDocument.TIPO_MENSAJE)).thenReturn(4L);
        when(repository.countByDestinatarioIdAndLeidaFalseAndTipo(usuarioId, NotificacionDocument.TIPO_MENSAJE)).thenReturn(2L);

        assertThat(service.contarNoLeidas(usuarioId)).isEqualTo(new NoLeidasDto(4, 2));
    }

    @Test
    void marcarTodasNoTocaLosMensajes() {
        assertThat(filtroDe(false)).contains("$ne", NotificacionDocument.TIPO_MENSAJE);
    }

    @Test
    void puedeMarcarSoloLosMensajes() {
        assertThat(filtroDe(true)).doesNotContain("$ne").contains(NotificacionDocument.TIPO_MENSAJE);
    }
}
