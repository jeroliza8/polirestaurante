package com.polirestaurante.service.integracion;

public record RespuestaPasarela(boolean aprobado, String codigoAutorizacion, String mensaje) {
}
