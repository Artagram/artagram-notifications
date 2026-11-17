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

class NotificationEventListenerBordesTest {

    private final NotificacionSseService sse = mock(NotificacionSseService.class);
    private final NotificationEventListener listener = new NotificationEventListener(sse);
    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();

    private NotificacionDocument enviada() {
        ArgumentCaptor<NotificacionDocument> c = ArgumentCaptor.forClass(NotificacionDocument.class);
        verify(sse).emitir(c.capture());
        return c.getValue();
    }

    @Test
    void nuevoSeguidor() {
        listener.onFollowCreated(Map.of("seguidoId", a.toString(), "seguidorId", b.toString(), "seguidorNombre", "sam"));
        NotificacionDocument n = enviada();
        assertThat(n.getTipo()).isEqualTo("NUEVO_SEGUIDOR");
        assertThat(n.getMensaje()).isEqualTo("@sam comenzó a seguirte");
    }

    @Test
    void planPremiumActivado() {
        listener.onSubscriptionUpdated(Map.of("usuarioId", a.toString(), "premium", true));
        assertThat(enviada().getTipo()).isEqualTo("SUSCRIPCION_PREMIUM");
    }

    @Test
    void mensajeDirecto() {
        listener.onPersonalMessageNotification(Map.of("destinatarioId", a.toString(), "actorId", b.toString(),
                "actorNombre", "sam", "referenciaId", "m1", "contenido", "hola"));
        NotificacionDocument n = enviada();
        assertThat(n.getTipo()).isEqualTo(NotificacionDocument.TIPO_MENSAJE);
        assertThat(n.getMensaje()).isEqualTo("Te envió un mensaje: hola");
    }

    @Test
    void likePropioYPayloadsInvalidosNoNotifican() {
        listener.onLikeCreated(Map.of("autorId", a.toString(), "usuarioId", a.toString(), "publicacionId", "p"));
        Map<String, Object> roto = Map.of("x", "y");
        listener.onFollowCreated(roto);
        listener.onLikeCreated(roto);
        listener.onCommentCreated(roto);
        listener.onSubscriptionUpdated(roto);
        listener.onAiCompleted(roto);
        listener.onPostModerated(roto);
        listener.onPersonalMessageNotification(roto);
        verifyNoInteractions(sse);
    }
}
