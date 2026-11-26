package com.polirestaurante.service.pago;

public record SolicitudCobro(Integer pedidoId, double total, Double montoRecibido, String referencia) {
}
