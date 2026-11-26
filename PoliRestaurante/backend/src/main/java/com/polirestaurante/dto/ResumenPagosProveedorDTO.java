package com.polirestaurante.dto;

import java.util.List;

/** RF36: estado de los pagos del restaurante al proveedor. */
public record ResumenPagosProveedorDTO(double totalPendiente, double totalPagado, List<PagoProveedorDTO> pagos) {
}
