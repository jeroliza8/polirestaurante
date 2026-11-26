package com.polirestaurante.dto;

import jakarta.validation.constraints.NotBlank;

public record AplicarPromocionRequest(@NotBlank(message = "Seleccione una promoción") String codigo) {
}
