package com.polirestaurante.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** RF34: el proveedor actualiza precio y disponibilidad de lo que suministra. */
public record CatalogoRequest(
        @NotNull(message = "El precio es obligatorio") @Positive(message = "El precio debe ser mayor a 0") Double precio,
        @NotNull(message = "Indique la disponibilidad") Boolean disponible) {
}
