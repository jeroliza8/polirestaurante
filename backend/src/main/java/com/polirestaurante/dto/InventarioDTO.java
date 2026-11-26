package com.polirestaurante.dto;

import java.util.List;

public record InventarioDTO(List<InsumoDTO> insumos, List<ProductoDTO> productos) {
}
