package com.polirestaurante.entity;

import com.polirestaurante.enums.Rol;

/**
 * Patron Factory (seccion 15.4 del documento): abstraccion comun para los cinco
 * tipos de empleado, cada uno guardado en su propia tabla de rol.
 */
public interface Empleado {
    Integer getUsuarioId();
    void setUsuarioId(Integer usuarioId);
    Rol getRol();
}
