package com.polirestaurante.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** CU-ADM-09: datos del proveedor, su acceso al sistema y los insumos que suministra. */
public record ProveedorRequest(
        @NotBlank(message = "El nombre del proveedor es obligatorio") @Size(max = 100) String nombre,
        @NotBlank(message = "La información de contacto es obligatoria") @Size(max = 100) String contacto,
        @NotBlank(message = "El usuario de acceso es obligatorio")
        @Pattern(regexp = "[A-Za-z0-9._-]{3,50}", message = "El usuario debe tener de 3 a 50 caracteres: letras, números, punto, guion")
        String usuario,
        @NotBlank(message = "La contraseña es obligatoria") @Size(min = 6, max = 72, message = "La contraseña debe tener entre 6 y 72 caracteres") String password,
        List<@Valid InsumoPrecio> insumos) {
}
