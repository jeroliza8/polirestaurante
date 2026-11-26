package com.polirestaurante.service.observer;

import com.polirestaurante.enums.EstadoPedido;

/** Notificación de dominio; no contiene credenciales ni es una entidad persistente. */
public record EventoCocina(String tipo, Integer idPedido, Integer meseroId, EstadoPedido estado,
                           Integer idProducto, Boolean disponible) {
    public static EventoCocina pedido(Integer id, Integer mesero, EstadoPedido estado) {
        return new EventoCocina(estado == EstadoPedido.LISTO ? "PEDIDO_LISTO" : "PEDIDO_ACTUALIZADO", id, mesero, estado, null, null);
    }
    public static EventoCocina disponibilidad(Integer id, boolean disponible) {
        return new EventoCocina("DISPONIBILIDAD_ACTUALIZADA", null, null, null, id, disponible);
    }
}
