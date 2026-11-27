package com.polirestaurante.dto;

import com.polirestaurante.enums.EstadoPedido;

import java.time.LocalDateTime;
import java.util.List;

/** RF14 / CU-CAJ-003: productos de la cuenta con su valor total. */
public record CuentaDetalleDTO(Integer pedidoId, Integer mesa, String tipoPedido, EstadoPedido estado, String mesero,
                               LocalDateTime fechaCreacion, List<ItemCuentaDTO> items, double subtotal,
                               double descuento, PromocionDTO promocion, double impuesto, String impuestoNombre,
                               double total) {
}
