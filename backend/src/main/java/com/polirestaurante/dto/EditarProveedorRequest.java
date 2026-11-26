package com.polirestaurante.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EditarProveedorRequest(
        @NotBlank(message = "El nombre del proveedor es obligatorio") @Size(max = 100) String nombre,
        @NotBlank(message = "La información de contacto es obligatoria") @Size(max = 100) String contacto) {
}
