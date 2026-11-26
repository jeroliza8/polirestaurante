package com.polirestaurante.dto;

import java.util.List;

public record ProveedorDTO(Integer id, String nombre, String contacto, String usuario, boolean usuarioActivo,
                           List<InsumoSuministradoDTO> insumos, double saldoPendiente) {
}
