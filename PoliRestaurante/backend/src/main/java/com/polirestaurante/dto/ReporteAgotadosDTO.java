package com.polirestaurante.dto;

import java.time.LocalDate;
import java.util.List;

/** CU-ADM-08 / RF31: reporte diario de agotados o con baja disponibilidad. */
public record ReporteAgotadosDTO(LocalDate fecha, List<InsumoDTO> insumos, List<ProductoDTO> productos, int totalAlertas) {
}
