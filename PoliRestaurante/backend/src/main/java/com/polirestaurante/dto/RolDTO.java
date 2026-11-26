package com.polirestaurante.dto;

import com.polirestaurante.enums.Rol;

import java.util.List;

public record RolDTO(Rol rol, String descripcion, List<String> permisos) {
}
