package com.artagram.notifications.notificacion;

import com.artagram.notifications.auth.JwtUserResolver;
import com.artagram.notifications.notificacion.dto.NoLeidasDto;
import com.artagram.notifications.notificacion.dto.NotificacionDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@Tag(name = "Notificaciones", description = "Centro de notificaciones y stream SSE en vivo")
public class NotificacionController {

    private final NotificacionRepository notificacionRepository;
    private final NotificacionSseService sseService;
    private final NotificacionLecturaService lecturaService;
    private final JwtUserResolver userResolver;

    public NotificacionController(NotificacionRepository notificacionRepository,
                                  NotificacionSseService sseService,
                                  NotificacionLecturaService lecturaService,
                                  JwtUserResolver userResolver) {
        this.notificacionRepository = notificacionRepository;
        this.sseService = sseService;
        this.lecturaService = lecturaService;
        this.userResolver = userResolver;
    }

    /** Los mensajes directos tienen su propio contador en la navegacion, no salen en este listado. */
    @GetMapping("/api/notificaciones")
    public List<NotificacionDto> listar(HttpServletRequest request) {
        var user = userResolver.resolverRequerido(request);
        return notificacionRepository
                .findTop50ByDestinatarioIdAndTipoNotOrderByCreadoEnDesc(user.usuarioId(), NotificacionDocument.TIPO_MENSAJE).stream()
                .map(n -> new NotificacionDto(
                        n.getId(), n.getDestinatarioId(), n.getTipo(), n.getActorId(),
                        n.getActorNombre(), n.getReferenciaId(), n.getMensaje(),
                        n.isLeida(), n.getCreadoEn()
                ))
                .toList();
    }

    @GetMapping("/api/notificaciones/no-leidas/cantidad")
    public NoLeidasDto noLeidas(HttpServletRequest request) {
        return lecturaService.contarNoLeidas(userResolver.resolverRequerido(request).usuarioId());
    }

    @PostMapping("/api/notificaciones/leer-todas")
    public ResponseEntity<Void> marcarTodas(@RequestParam(defaultValue = "false") boolean mensajes, HttpServletRequest request) {
        lecturaService.marcarTodas(userResolver.resolverRequerido(request).usuarioId(), mensajes);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/notificaciones/{id}/leer")
    public ResponseEntity<Void> marcarLeida(@PathVariable UUID id, HttpServletRequest request) {
        var user = userResolver.resolverRequerido(request);
        notificacionRepository.findById(id).ifPresent(n -> {
            if (n.getDestinatarioId().equals(user.usuarioId())) {
                n.setLeida(true);
                notificacionRepository.save(n);
            }
        });
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/api/notificaciones/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(HttpServletRequest request) {
        var user = userResolver.resolverRequerido(request);
        return sseService.registrar(user.usuarioId());
    }
}
