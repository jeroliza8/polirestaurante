package com.polirestaurante.dto;

import com.polirestaurante.enums.MedioPago;
import com.polirestaurante.enums.TipoFactura;

import java.time.LocalDateTime;
import java.util.List;

/** Factura / comprobante para imprimir o reimprimir (RF19-RF21). */
public record FacturaDTO(Integer pagoId, String numeroFactura, TipoFactura tipoFactura, LocalDateTime fechaFactura,
                         LocalDateTime fechaPago, Integer pedidoId, Integer mesa, String cajero, MedioPago medioPago,
                         String referencia, Double montoRecibido, Double cambio, List<ItemCuentaDTO> items,
                         double subtotal, double descuento, String promocion, double impuesto,
                         String impuestoNombre, double total, String codigoFacturaElectronica,
                         String correoCliente, String advertencia) {
}
