package com.polirestaurante.dto;

import com.polirestaurante.enums.Rol;

public record LoginResponse(String token, Integer usuarioId, String nombre, Rol rol, long expiraEnSegundos) {
}
