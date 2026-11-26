package com.polirestaurante.dto;

public record ItemSolicitudDTO(Integer insumoId, String insumo, String unidadMedida, Integer cantidad,
                               Double precioUnitario, double subtotal) {
}
