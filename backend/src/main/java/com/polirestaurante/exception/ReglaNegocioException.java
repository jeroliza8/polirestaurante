package com.polirestaurante.exception;

/** Regla de negocio incumplida -> HTTP 400 con el mensaje del caso de uso. */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
