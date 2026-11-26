package com.polirestaurante.dto;

import java.util.List;

/** estado: OK, BAJO o AGOTADO. */
public record InsumoDTO(Integer id, String nombre, String unidadMedida, Integer cantidadDisponible,
                        Integer stockMinimo, String estado, List<String> proveedores) {
}
