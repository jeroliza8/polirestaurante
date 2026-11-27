package com.polirestaurante.dto;

import com.polirestaurante.enums.EstadoPedido;

import java.time.LocalDateTime;

public record CuentaResumenDTO(Integer pedidoId, Integer mesa, String tipoPedido, EstadoPedido estado, String mesero,
                               LocalDateTime fechaCreacion, int productos, double total, String promocion) {
}
