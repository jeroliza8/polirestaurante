package com.polirestaurante.enums;

import java.util.List;

public enum EstadoPedido {
    PENDIENTE, EN_PREPARACION, LISTO, ENTREGADO, CANCELADO, PAGADO;

    /** Una cuenta cerrada ya no se puede cobrar, anular ni modificar. */
    public static final List<EstadoPedido> CERRADOS = List.of(PAGADO, CANCELADO);

    public boolean cerrado() {
        return CERRADOS.contains(this);
    }
}
