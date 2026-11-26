package com.polirestaurante.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SolicitudRequest(
        @NotNull(message = "Seleccione el proveedor") Integer proveedorId,
        @NotEmpty(message = "Agregue al menos un insumo") List<@Valid ItemSolicitud> items) {
}
