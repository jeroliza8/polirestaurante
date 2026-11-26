package com.polirestaurante.dto;

public record InsumoSuministradoDTO(Integer idRelacion, Integer insumoId, String insumo, String unidadMedida,
                                    Double precio, boolean disponible) {
}
