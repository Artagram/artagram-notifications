package com.artagram.notifications.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.artagram.notifications.notificacion.NotificacionDocument;
import com.artagram.notifications.notificacion.NotificacionSseService;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NotificationEventListenerTest {

    private final NotificacionSseService sse = mock(NotificacionSseService.class);
    private final NotificationEventListener listener = new NotificationEventListener(sse);
    private final UUID autorId = UUID.randomUUID();
    private final String obraId = UUID.randomUUID().toString();

    private NotificacionDocument moderar(String accion) {
        listener.onPostModerated(Map.of("autorId", autorId.toString(), "publicacionId", obraId, "titulo", "Mar", "accion", accion));
        ArgumentCaptor<NotificacionDocument> enviada = ArgumentCaptor.forClass(NotificacionDocument.class);
        verify(sse).emitir(enviada.capture());
        return enviada.getValue();
    }

    @Test
    void avisaAlAutorCuandoSuObraSeOculta() {
        NotificacionDocument n = moderar("OCULTADA");

        assertThat(n.getDestinatarioId()).isEqualTo(autorId);
        assertThat(n.getTipo()).isEqualTo("OBRA_MODERADA");
        assertThat(n.getReferenciaId()).isEqualTo(obraId);
        assertThat(n.getMensaje()).contains("Ocultamos", "Mar");
    }

    @Test
    void avisaAlAutorCuandoSuObraVuelve() {
        assertThat(moderar("RESTAURADA").getMensaje()).contains("visible de nuevo");
    }
}
