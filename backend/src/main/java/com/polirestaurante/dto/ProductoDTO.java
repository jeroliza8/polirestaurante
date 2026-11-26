package com.polirestaurante.dto;

/** estadoStock: OK, BAJO o AGOTADO. */
public record ProductoDTO(Integer id, String nombre, String descripcion, Double precio, Integer cantidadDisponible,
                          Integer stockMinimo, boolean disponible, Integer categoriaId, String categoria,
                          String estadoStock) {
}
