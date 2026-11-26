package com.polirestaurante.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ItemSolicitud(
        @NotNull(message = "Seleccione el insumo") Integer insumoId,
        @NotNull(message = "La cantidad es obligatoria") @Positive(message = "La cantidad debe ser mayor a 0") Integer cantidad) {
}
