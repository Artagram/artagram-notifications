package com.artagram.notifications.notificacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.artagram.notifications.auth.JwtService;
import com.artagram.notifications.auth.JwtUserResolver;
import com.artagram.notifications.common.error.ApiException;
import com.artagram.notifications.common.error.GlobalExceptionHandler;
import com.artagram.notifications.notificacion.dto.NoLeidasDto;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class NotificacionControllerTest {

    private final UUID yo = UUID.randomUUID();
    private NotificacionRepository repo;
    private NotificacionSseService sse;
    private NotificacionLecturaService lectura;
    private JwtUserResolver resolver;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        repo = mock(NotificacionRepository.class);
        sse = mock(NotificacionSseService.class);
        lectura = mock(NotificacionLecturaService.class);
        resolver = mock(JwtUserResolver.class);
        when(resolver.resolverRequerido(any())).thenReturn(new JwtService.TokenPrincipal(yo, "lucia", "USUARIO", false));
        mvc = MockMvcBuilders.standaloneSetup(new NotificacionController(repo, sse, lectura, resolver))
                .setControllerAdvice(new GlobalExceptionHandler(new SimpleMeterRegistry()))
                .build();
    }

    @Test
    void listaCuentaYMarcaComoLeidas() throws Exception {
        NotificacionDocument n = new NotificacionDocument(null, yo, "NUEVO_LIKE", UUID.randomUUID(), "sam", "obra", "le gustó");
        when(repo.findTop50ByDestinatarioIdAndTipoNotOrderByCreadoEnDesc(yo, NotificacionDocument.TIPO_MENSAJE)).thenReturn(List.of(n));
        when(lectura.contarNoLeidas(yo)).thenReturn(new NoLeidasDto(3, 1));

        mvc.perform(get("/api/notificaciones")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("NUEVO_LIKE"));
        mvc.perform(get("/api/notificaciones/no-leidas/cantidad")).andExpect(status().isOk());
        mvc.perform(post("/api/notificaciones/leer-todas").param("mensajes", "true")).andExpect(status().isNoContent());
        verify(lectura).marcarTodas(yo, true);
    }

    @Test
    void marcarLeidaSoloAfectaNotificacionesPropias() throws Exception {
        NotificacionDocument mia = new NotificacionDocument(null, yo, "X", null, "a", null, "m");
        NotificacionDocument ajena = new NotificacionDocument(null, UUID.randomUUID(), "X", null, "a", null, "m");
        when(repo.findById(mia.getId())).thenReturn(Optional.of(mia));
        when(repo.findById(ajena.getId())).thenReturn(Optional.of(ajena));

        mvc.perform(post("/api/notificaciones/" + mia.getId() + "/leer")).andExpect(status().isOk());
        mvc.perform(post("/api/notificaciones/" + ajena.getId() + "/leer")).andExpect(status().isOk());
        mvc.perform(post("/api/notificaciones/" + UUID.randomUUID() + "/leer")).andExpect(status().isOk());

        assertThat(mia.isLeida()).isTrue();
        assertThat(ajena.isLeida()).isFalse();
        verify(repo).save(mia);
        verify(repo, never()).save(ajena);
    }

    @Test
    void streamRegistraElEmitter() throws Exception {
        when(sse.registrar(yo)).thenReturn(new SseEmitter());
        mvc.perform(get("/api/notificaciones/stream")).andExpect(status().isOk());
        verify(sse).registrar(yo);
    }

    @Test
    void erroresUsanElFormatoComun() throws Exception {
        when(lectura.contarNoLeidas(yo))
                .thenThrow(ApiException.tooManyRequests("LIMITE", "calma"))
                .thenThrow(new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "CAIDO", "x"))
                .thenThrow(new IllegalStateException("boom"));

        mvc.perform(get("/api/notificaciones/no-leidas/cantidad")).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("LIMITE"));
        mvc.perform(get("/api/notificaciones/no-leidas/cantidad")).andExpect(status().isServiceUnavailable());
        mvc.perform(get("/api/notificaciones/no-leidas/cantidad")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("ERROR_INTERNO"));
        mvc.perform(put("/api/notificaciones")).andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METODO_NO_PERMITIDO"));
        mvc.perform(post("/api/notificaciones/no-uuid/leer")).andExpect(status().isInternalServerError());
    }
}
