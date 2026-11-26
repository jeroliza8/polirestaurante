package com.polirestaurante.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MesaRequest(
        @NotNull(message = "El número de mesa es obligatorio") @Min(value = 1, message = "El número de mesa debe ser mayor a 0") Integer numero,
        @NotNull(message = "La capacidad es obligatoria") @Min(value = 1, message = "La capacidad mínima es 1")
        @Max(value = 30, message = "La capacidad máxima es 30") Integer capacidad) {
}
