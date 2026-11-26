package com.polirestaurante.dto;

import com.polirestaurante.enums.Rol;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CambioRolRequest(
        @NotNull(message = "Seleccione un rol") Rol rol,
        @Size(max = 45) String estacion,
        @Size(max = 45) String zonaAsignada,
        @Size(max = 45) String placaVehiculo) {

    public DatosRol datosRol() {
        return new DatosRol(estacion, zonaAsignada, placaVehiculo);
    }
}
