package com.polirestaurante.exception;

/** HTTP 401 en el inicio de sesion. */
public class AutenticacionException extends RuntimeException {
    public AutenticacionException(String mensaje) {
        super(mensaje);
    }
}
