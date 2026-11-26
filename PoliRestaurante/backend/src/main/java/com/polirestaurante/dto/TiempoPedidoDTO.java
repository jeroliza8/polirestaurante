package com.polirestaurante.dto;

import java.time.LocalDateTime;

/** Minutos de cada etapa; null si el pedido aun no llega a esa etapa. */
public record TiempoPedidoDTO(Integer pedidoId, Integer mesa, String estado, LocalDateTime fechaCreacion,
                              Double minutosEspera, Double minutosPreparacion, Double minutosEntrega,
                              Double minutosTotal) {
}
