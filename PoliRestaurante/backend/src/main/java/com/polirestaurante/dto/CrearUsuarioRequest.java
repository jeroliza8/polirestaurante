package com.polirestaurante.dto;

import com.polirestaurante.enums.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CrearUsuarioRequest(
        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Pattern(regexp = "[A-Za-z0-9._-]{3,50}", message = "El usuario debe tener de 3 a 50 caracteres: letras, números, punto, guion")
        String nombre,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, max = 72, message = "La contraseña debe tener entre 6 y 72 caracteres")
        String password,
        @NotNull(message = "Seleccione un rol") Rol rol,
        @Size(max = 45) String estacion,
        @Size(max = 45) String zonaAsignada,
        @Size(max = 45) String placaVehiculo) {

    public DatosRol datosRol() {
        return new DatosRol(estacion, zonaAsignada, placaVehiculo);
    }
}
