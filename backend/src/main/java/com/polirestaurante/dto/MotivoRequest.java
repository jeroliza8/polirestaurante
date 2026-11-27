package com.polirestaurante.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MotivoRequest(
        @NotBlank(message = "Indique el motivo")
        @Size(min = 3, max = 255, message = "El motivo debe tener entre 3 y 255 caracteres") String motivo) {
}
