package com.polirestaurante.dto;

import java.time.LocalDateTime;
import java.util.List;

/** CU-ADM-01: monto total, numero de pedidos y producto mas vendido del periodo. */
public record ReporteVentasDTO(
        LocalDateTime desde, LocalDateTime hasta, String categoria,
        double montoTotal, double descuentos, long numeroPedidos, double ticketPromedio,
        String productoMasVendido, long unidadesProductoMasVendido,
        List<ItemVendido> topProductos,
        List<FilaVentaDia> ventasPorDia,
        List<SerieValor> ventasPorCategoria,
        List<SerieValor> ventasPorMedioPago) {
}
