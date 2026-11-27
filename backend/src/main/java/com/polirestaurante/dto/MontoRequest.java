package com.polirestaurante.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record MontoRequest(
        @NotNull(message = "El monto es obligatorio") @PositiveOrZero(message = "El monto no puede ser negativo") Double monto) {
}
