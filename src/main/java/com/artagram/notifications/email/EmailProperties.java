package com.artagram.notifications.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * El enlace de verificacion ya viene armado desde artagram-auth (que sigue
 * siendo dueño del token/expiracion) via el evento user.verification.requested
 * — aqui solo se decide el modo de envio (log/smtp) y el remitente.
 */
@ConfigurationProperties(prefix = "app.email")
public class EmailProperties {

    private String mode = "log";
    private String from;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }
}
