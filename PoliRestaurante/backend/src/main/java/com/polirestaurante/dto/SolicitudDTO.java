package com.polirestaurante.dto;

import com.polirestaurante.enums.EstadoSolicitud;

import java.time.LocalDateTime;
import java.util.List;

/** estadoPago: null si aun no se recibe; PENDIENTE o PAGADO despues. */
public record SolicitudDTO(Integer id, LocalDateTime fecha, EstadoSolicitud estado, Integer proveedorId,
                           String proveedor, List<ItemSolicitudDTO> items, double total,
                           LocalDateTime fechaRecepcion, String estadoPago) {
}
