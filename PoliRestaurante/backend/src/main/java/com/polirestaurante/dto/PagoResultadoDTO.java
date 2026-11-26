package com.polirestaurante.dto;

import com.polirestaurante.enums.MedioPago;

public record PagoResultadoDTO(Integer pagoId, Integer pedidoId, String mensaje, MedioPago medioPago, double total,
                               Double montoRecibido, Double cambio, String referencia) {
}
