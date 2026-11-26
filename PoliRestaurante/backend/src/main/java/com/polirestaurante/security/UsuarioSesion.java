package com.polirestaurante.security;

import com.polirestaurante.enums.Rol;

/** Datos del usuario autenticado que viajan en el JWT. */
public record UsuarioSesion(Integer id, String nombre, Rol rol) {
}
