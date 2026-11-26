package com.polirestaurante.dto;

import jakarta.validation.constraints.*;

public record ProductoRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 100, message = "El nombre admite máximo 100 caracteres") String nombre,
        @Size(max = 100, message = "La descripción admite máximo 100 caracteres") String descripcion,
        @NotNull(message = "El precio es obligatorio") @Positive(message = "El precio debe ser mayor a 0") Double precio,
        @NotNull(message = "La cantidad disponible es obligatoria") @PositiveOrZero(message = "La cantidad no puede ser negativa") Integer cantidadDisponible,
        @NotNull(message = "Seleccione una categoría") Integer categoriaId,
        Boolean disponible,
        @PositiveOrZero(message = "El stock mínimo no puede ser negativo") Integer stockMinimo) {
}
