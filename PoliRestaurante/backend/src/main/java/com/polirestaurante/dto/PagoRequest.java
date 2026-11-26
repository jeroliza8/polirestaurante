package com.polirestaurante.dto;

import com.polirestaurante.enums.MedioPago;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * HU-CAJ-001. montoRecibido aplica al efectivo; referencia a tarjeta (ultimos 4
 * digitos, nunca el numero completo) o transferencia (codigo del banco).
 */
public record PagoRequest(
        @NotNull(message = "Seleccione la cuenta") Integer pedidoId,
        @NotNull(message = "Seleccione el medio de pago") MedioPago medioPago,
        @PositiveOrZero(message = "El monto recibido no puede ser negativo") Double montoRecibido,
        @Size(max = 40, message = "Referencia demasiado larga") String referencia) {
}
