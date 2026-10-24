package com.artagram.notifications.common.tracing;

/**
 * Claves de MDC compartidas entre el filtro HTTP y los listeners de eventos, para que
 * un mismo flujo deje logs correlacionados.
 */
public final class TraceContext {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";
    public static final String USER_ID_MDC_KEY = "usuarioId";
    public static final String OPERATION_MDC_KEY = "operacion";

    private TraceContext() {
    }
}
