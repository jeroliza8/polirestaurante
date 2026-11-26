package com.polirestaurante.exception;

/** HU-CAJ-001 AC4: "pago rechazado" -> HTTP 422; el cajero puede reintentar. */
public class PagoRechazadoException extends RuntimeException {
    public PagoRechazadoException(String mensaje) {
        super(mensaje);
    }
}
