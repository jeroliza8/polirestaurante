package com.polirestaurante.dto;

import com.polirestaurante.enums.EstadoDetalle;

public record ItemCuentaDTO(Integer detalleId, Integer productoId, String producto, Integer categoriaId,
                            Double precioUnitario, Integer cantidad, String notas, double subtotal,
                            EstadoDetalle estado, String motivoAnulacion) {
}
