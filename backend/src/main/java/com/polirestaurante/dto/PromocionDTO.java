package com.polirestaurante.dto;

import java.time.LocalDate;

public record PromocionDTO(String codigo, String nombre, String descripcion, double porcentaje, Integer categoriaId,
                           String categoria, LocalDate vigenteHasta, Double descuentoEstimado) {
}
