package com.polirestaurante.dto;

import com.polirestaurante.enums.MedioPago;
import com.polirestaurante.enums.TipoFactura;

import java.time.LocalDateTime;

/**
 * RF18: historial con el metodo de pago.
 * estadoFactura: EMITIDA, SIN_EMITIR (pagada pero sin comprobante) o PENDIENTE_FE
 * (electronica a la espera de respuesta del proveedor externo).
 */
public record FacturaResumenDTO(Integer pagoId, String numeroFactura, TipoFactura tipoFactura, String estadoFactura,
                                LocalDateTime fechaPago, Integer pedidoId, Integer mesa, String cajero,
                                MedioPago medioPago, double total) {
}
