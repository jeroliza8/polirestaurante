package com.polirestaurante.service.state;

import com.polirestaurante.enums.EstadoPedido;
import com.polirestaurante.exception.ReglaNegocioException;

/** State (documento, pp. 88-89). El enum compartido sigue siendo el valor persistido. */
public interface EstadoCocina {
    EstadoPedido avanzar();
    EstadoPedido cancelar();

    static EstadoCocina de(EstadoPedido estado) {
        if (estado == null) throw new ReglaNegocioException("El pedido no tiene un estado válido.");
        return switch (estado) {
            case PENDIENTE -> new PendienteState();
            case EN_PREPARACION -> new EnPreparacionState();
            case LISTO -> new ListoState();
            case ENTREGADO -> new EntregadoState();
            case CANCELADO -> new CanceladoState();
            case PAGADO -> new PagadoState();
        };
    }

    /** El rol COCINERO únicamente puede realizar los dos avances de preparación. */
    static EstadoPedido validarTransicion(EstadoPedido actual, EstadoPedido destino) {
        if (actual == EstadoPedido.CANCELADO)
            throw new ReglaNegocioException("Pedido cancelado, no es posible modificarlo.");
        if (destino != EstadoPedido.EN_PREPARACION && destino != EstadoPedido.LISTO)
            throw new ReglaNegocioException("Cocina solo puede iniciar o finalizar la preparación.");
        if (de(actual).avanzar() != destino)
            throw new ReglaNegocioException(destino == EstadoPedido.LISTO
                ? "El pedido debe estar en preparación antes de marcarse como listo."
                : "Solo se puede iniciar un pedido pendiente.");
        return destino;
    }
}
abstract class EstadoSinCambio implements EstadoCocina {
    @Override public EstadoPedido avanzar() { throw new ReglaNegocioException("El pedido ya no permite ese cambio de estado."); }
    @Override public EstadoPedido cancelar() { throw new ReglaNegocioException("El pedido no se puede cancelar en su estado actual."); }
}
final class PendienteState extends EstadoSinCambio {
    @Override public EstadoPedido avanzar() { return EstadoPedido.EN_PREPARACION; }
    @Override public EstadoPedido cancelar() { return EstadoPedido.CANCELADO; }
}
final class EnPreparacionState extends EstadoSinCambio {
    @Override public EstadoPedido avanzar() { return EstadoPedido.LISTO; }
    @Override public EstadoPedido cancelar() { return EstadoPedido.CANCELADO; }
}
final class ListoState extends EstadoSinCambio {
    @Override public EstadoPedido avanzar() { return EstadoPedido.ENTREGADO; }
}
final class EntregadoState extends EstadoSinCambio {}
final class CanceladoState extends EstadoSinCambio {}
final class PagadoState extends EstadoSinCambio {} // estado adicional de la rama admin-juliana
