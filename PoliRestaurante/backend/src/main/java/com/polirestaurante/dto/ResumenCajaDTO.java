package com.polirestaurante.dto;

import com.polirestaurante.enums.EstadoCaja;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Apertura / cierre de caja con el reporte de ventas del turno y el
 * "C-producto" (inventario de lo vendido) que pide el actor Cajero.
 */
public record ResumenCajaDTO(Integer idCaja, EstadoCaja estado, LocalDateTime fechaApertura, LocalDateTime fechaCierre,
                             Double montoInicial, Double montoFinal, double totalVentas, long numeroPagos,
                             List<SerieValor> ventasPorMedioPago, double efectivoEsperado, Double diferencia,
                             List<ItemVendido> productosVendidos) {
}
