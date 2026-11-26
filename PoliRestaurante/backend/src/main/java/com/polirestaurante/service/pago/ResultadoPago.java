package com.polirestaurante.service.pago;

public record ResultadoPago(boolean aprobado, String mensaje, Double montoRecibido, Double cambio, String referencia) {

    public static ResultadoPago aprobado(Double montoRecibido, Double cambio, String referencia) {
        return new ResultadoPago(true, "Pago aprobado", montoRecibido, cambio, referencia);
    }

    public static ResultadoPago rechazado(String motivo) {
        return new ResultadoPago(false, "Pago rechazado: " + motivo, null, null, null);
    }
}
