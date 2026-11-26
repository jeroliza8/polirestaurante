package com.polirestaurante.exception;

import java.util.Map;

/** HTTP 409: dato duplicado. Puede llevar datos extra, p. ej. el siguiente numero de mesa sugerido. */
public class ConflictoException extends RuntimeException {
    private final Map<String, Object> datos;

    public ConflictoException(String mensaje) {
        this(mensaje, Map.of());
    }

    public ConflictoException(String mensaje, Map<String, Object> datos) {
        super(mensaje);
        this.datos = datos;
    }

    public Map<String, Object> getDatos() {
        return datos;
    }
}
