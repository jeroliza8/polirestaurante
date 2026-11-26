package com.polirestaurante.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InsumoPrecio(
        @NotNull(message = "Seleccione el insumo") Integer insumoId,
        @NotNull(message = "El precio es obligatorio") @Positive(message = "El precio debe ser mayor a 0") Double precio) {
}
