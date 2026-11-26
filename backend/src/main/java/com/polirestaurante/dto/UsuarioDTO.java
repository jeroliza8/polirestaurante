package com.polirestaurante.dto;

import com.polirestaurante.enums.EstadoUsuario;
import com.polirestaurante.enums.Rol;

/** "detalle" = zona del mesero, estacion del cocinero, placa del repartidor o nombre del proveedor. */
public record UsuarioDTO(Integer id, String nombre, Rol rol, EstadoUsuario estado, String detalle) {
}
