package com.polirestaurante.dto;

import jakarta.validation.constraints.*;

public record InsumoRequest(
        @NotBlank(message = "El nombre del insumo es obligatorio") @Size(max = 100) String nombre,
        @NotBlank(message = "La unidad de medida es obligatoria") @Size(max = 20) String unidadMedida,
        @NotNull(message = "La cantidad es obligatoria") @PositiveOrZero(message = "La cantidad no puede ser negativa") Integer cantidadDisponible,
        @NotNull(message = "El stock mínimo es obligatorio") @PositiveOrZero(message = "El stock mínimo no puede ser negativo") Integer stockMinimo) {
}
