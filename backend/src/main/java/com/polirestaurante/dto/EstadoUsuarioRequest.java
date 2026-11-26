package com.polirestaurante.dto;

import jakarta.validation.constraints.NotNull;

public record EstadoUsuarioRequest(@NotNull(message = "Indique si el usuario queda activo") Boolean activo) {
}
