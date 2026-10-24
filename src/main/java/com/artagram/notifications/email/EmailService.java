package com.artagram.notifications.email;

public interface EmailService {

    void enviarVerificacion(String correoDestino, String nombreArtista, String enlaceVerificacion);
}
