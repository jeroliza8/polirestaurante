package com.polirestaurante.dto;

import java.time.LocalDateTime;
import java.util.List;

/** CU-ADM-06 / RF29: tiempos por pedido y por etapa. */
public record ReporteTiemposDTO(LocalDateTime desde, LocalDateTime hasta, int pedidosAnalizados,
                                Double promedioEspera, Double promedioPreparacion, Double promedioEntrega,
                                Double promedioTotal, List<TiempoPedidoDTO> pedidos) {
}
