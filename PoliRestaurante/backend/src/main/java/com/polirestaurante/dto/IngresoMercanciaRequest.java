package com.polirestaurante.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * CU-ADM-10. Si se indica proveedor, el ingreso queda como una entrega de ese proveedor
 * (aparece en su historial, RF34) y genera el pago pendiente (RF36).
 */
public record IngresoMercanciaRequest(
        @NotNull(message = "La cantidad es obligatoria") @Positive(message = "La cantidad debe ser mayor a 0")
        @Max(value = 100000, message = "Cantidad fuera de rango") Integer cantidad,
        Integer proveedorId,
        @Positive(message = "El precio debe ser mayor a 0") Double precioUnitario) {
}
