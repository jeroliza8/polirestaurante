package com.polirestaurante.dto;

import com.polirestaurante.enums.EstadoPagoProveedor;

import java.time.LocalDateTime;

public record PagoProveedorDTO(Integer id, Integer solicitudId, Integer proveedorId, String proveedor, Double monto,
                               EstadoPagoProveedor estado, LocalDateTime fechaGenerado, LocalDateTime fechaPago) {
}
