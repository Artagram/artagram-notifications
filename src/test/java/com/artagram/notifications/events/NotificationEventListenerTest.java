package com.artagram.notifications.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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

    private NotificacionDocument enviada() {
        ArgumentCaptor<NotificacionDocument> enviada = ArgumentCaptor.forClass(NotificacionDocument.class);
        verify(sse).emitir(enviada.capture());
        return enviada.getValue();
    }

    @Test
    void elLikeUsaElEventoRealDeFeed() {
        listener.onLikeCreated(Map.of("publicacionId", obraId, "autorId", autorId.toString(),
                "usuarioId", UUID.randomUUID().toString(), "usuarioNombre", "Ana"));

        NotificacionDocument n = enviada();
        assertThat(n.getReferenciaId()).isEqualTo(obraId);
        assertThat(n.getMensaje()).contains("@Ana");
    }

    @Test
    void elComentarioUsaElEventoRealDeFeed() {
        listener.onCommentCreated(Map.of("publicacionId", obraId, "publicacionAutorId", autorId.toString(),
                "autorId", UUID.randomUUID().toString(), "autorNombre", "Ana", "contenido", "Bella"));

        NotificacionDocument n = enviada();
        assertThat(n.getDestinatarioId()).isEqualTo(autorId);
        assertThat(n.getReferenciaId()).isEqualTo(obraId);
        assertThat(n.getMensaje()).contains("Bella");
    }

    @Test
    void comentarTuPropiaObraNoNotifica() {
        listener.onCommentCreated(Map.of("publicacionId", obraId, "publicacionAutorId", autorId.toString(),
                "autorId", autorId.toString(), "autorNombre", "Ana", "contenido", "x"));

        verifyNoInteractions(sse);
    }

    @Test
    void elAnalisisAvisaAQuienLoPidio() {
        listener.onAiCompleted(Map.of("publicacionId", obraId, "autorId", autorId.toString()));

        NotificacionDocument n = enviada();
        assertThat(n.getDestinatarioId()).isEqualTo(autorId);
        assertThat(n.getReferenciaId()).isEqualTo(obraId);
    }

    @Test
    void avisaCuandoElPlanVence() {
        listener.onSubscriptionUpdated(Map.of("usuarioId", autorId.toString(), "premium", false));

        assertThat(enviada().getTipo()).isEqualTo("SUSCRIPCION_VENCIDA");
    }
}
