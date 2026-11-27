package com.polirestaurante.service.mesero;

import com.polirestaurante.enums.EstadoPedido;
import com.polirestaurante.exception.ReglaNegocioException;

/**
 * Reglas puras del mesero (RF01-RF06, HU-MES-001/002/003, CU-MES-01/02).
 * Sin Spring ni base de datos, para probarlas con JUnit (RNF17), igual que CalculadoraCuenta.
 */
public final class ReglasMesero {
    public static final int CANTIDAD_MAXIMA = 100_000;

    private ReglasMesero() { }

    public static void validarCantidad(Integer cantidad) {
        if (cantidad == null || cantidad <= 0 || cantidad > CANTIDAD_MAXIMA)
            throw new ReglaNegocioException("La cantidad debe estar entre 1 y " + CANTIDAD_MAXIMA + ".");
    }

    /** Una cuenta sigue abierta hasta que se paga o se cancela (el cajero cierra las entregadas). */
    public static boolean cuentaAbierta(EstadoPedido estado) {
        return estado != null && !estado.cerrado();
    }

    /** RF02: solo se editan pedidos PENDIENTES (cocina todavía no los toma). */
    public static void exigirEditable(EstadoPedido estado) {
        exigirSinPago(estado);
        if (estado != EstadoPedido.PENDIENTE)
            throw new ReglaNegocioException("Solo se pueden editar pedidos pendientes.");
    }

    /** RF03: el mesero entrega solo lo que cocina dejó LISTO. */
    public static void validarEntrega(EstadoPedido estado) {
        if (estado == EstadoPedido.ENTREGADO) throw new ReglaNegocioException("El pedido ya fue entregado.");
        if (estado == EstadoPedido.CANCELADO) throw new ReglaNegocioException("El pedido está cancelado.");
        exigirSinPago(estado);
        if (estado != EstadoPedido.LISTO)
            throw new ReglaNegocioException("Solo se puede entregar un pedido que cocina ya dejó LISTO.");
    }

    /** RF04: solo PENDIENTE o EN_PREPARACION; entregado o pagado se rechaza con mensaje claro. */
    public static void validarCancelacion(EstadoPedido estado) {
        if (estado == EstadoPedido.ENTREGADO) throw new ReglaNegocioException("Un pedido entregado no puede cancelarse.");
        if (estado == EstadoPedido.CANCELADO) throw new ReglaNegocioException("El pedido ya está cancelado.");
        exigirSinPago(estado);
        if (estado != EstadoPedido.PENDIENTE && estado != EstadoPedido.EN_PREPARACION)
            throw new ReglaNegocioException("Solo se pueden cancelar pedidos pendientes o en preparación.");
    }

    /** Pendiente: nada se ha cocinado y las existencias vuelven. En preparación la comida ya se usó. */
    public static boolean devuelveExistencias(EstadoPedido estadoAnterior) {
        return estadoAnterior == EstadoPedido.PENDIENTE;
    }

    /** RF06: la mesa no cambia de estado mientras tenga cuentas abiertas. */
    public static void validarCambioMesa(boolean mesaActiva, long cuentasAbiertas) {
        if (!mesaActiva) throw new ReglaNegocioException("La mesa está desactivada.");
        if (cuentasAbiertas > 0)
            throw new ReglaNegocioException(
                "La mesa conserva pedidos activos o cuentas sin pagar; complete su flujo antes de cambiarla.");
    }

    /** RF01: crear pedido solo en mesa activa y sin cuenta abierta. */
    public static void validarMesaParaPedido(boolean mesaActiva, long cuentasAbiertas) {
        if (!mesaActiva || cuentasAbiertas > 0)
            throw new ReglaNegocioException("La mesa ya tiene una cuenta activa o está desactivada.");
    }

    /** Criterio de Cocina (docs/COCINA.md): activo, disponible y con existencias. */
    public static void validarProductoDisponible(String nombre, boolean activo, boolean disponible, Integer existencias) {
        if (!activo || !disponible || existencias == null || existencias <= 0)
            throw new ReglaNegocioException("El producto '" + nombre + "' no está disponible.");
    }

    private static void exigirSinPago(EstadoPedido estado) {
        if (estado == EstadoPedido.PAGADO)
            throw new ReglaNegocioException("El pedido tiene un pago confirmado y no se puede modificar.");
    }
}
