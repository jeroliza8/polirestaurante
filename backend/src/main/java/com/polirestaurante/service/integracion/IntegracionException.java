package com.polirestaurante.service.integracion;

/** Falla de un servicio externo (pasarela o proveedor de factura electronica). */
public class IntegracionException extends RuntimeException {
    public IntegracionException(String mensaje) {
        super(mensaje);
    }
}
