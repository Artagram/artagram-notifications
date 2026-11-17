package com.artagram.notifications.notificacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class NotificacionSseServiceTest {

    private final NotificacionRepository repo = mock(NotificacionRepository.class);
    private final NotificacionSseService service = new NotificacionSseService(repo);

    @SuppressWarnings("unchecked")
    private Map<UUID, List<SseEmitter>> subscriptores() throws Exception {
        Field f = NotificacionSseService.class.getDeclaredField("subscriptores");
        f.setAccessible(true);
        return (Map<UUID, List<SseEmitter>>) f.get(service);
    }

    @Test
    void sinSuscriptoresSoloGuarda() {
        NotificacionDocument doc = new NotificacionDocument(null, UUID.randomUUID(), "X", null, "a", null, "m");
        service.emitir(doc);
        service.enviarHeartbeat();
        verify(repo).save(doc);
    }

    @Test
    void losEmittersQueFallanSeDescartan() throws Exception {
        UUID usuario = UUID.randomUUID();
        SseEmitter emitter = service.registrar(usuario);
        assertThat(subscriptores()).containsKey(usuario);

        // Sin una respuesta HTTP real el envío falla y el emitter se descarta.
        emitter.complete();
        service.emitir(new NotificacionDocument(null, usuario, "X", null, "a", null, "m"));
        assertThat(subscriptores()).doesNotContainKey(usuario);

        service.registrar(usuario);
        service.registrar(usuario);
        service.enviarHeartbeat();
        subscriptores().getOrDefault(usuario, List.of()).forEach(SseEmitter::complete);
        service.enviarHeartbeat();
        assertThat(subscriptores()).doesNotContainKey(usuario);
    }
}
