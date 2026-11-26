package com.polirestaurante.dto;

import com.polirestaurante.enums.TipoFactura;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** HU-CAJ-001 AC5: el cliente elige factura fisica o electronica. */
public record FacturaRequest(
        @NotNull(message = "Seleccione el tipo de factura") TipoFactura tipoFactura,
        @Email(message = "Correo electrónico inválido") @Size(max = 100) String correoCliente) {
}
