package com.polirestaurante.service;

import com.polirestaurante.enums.EstadoPedido;
import com.polirestaurante.exception.ReglaNegocioException;
import com.polirestaurante.service.mesero.ReglasMesero;
import org.junit.jupiter.api.Test;
import static com.polirestaurante.enums.EstadoPedido.*;
import static org.junit.jupiter.api.Assertions.*;

/** Reglas del mesero (RF01-RF06). Sin MySQL ni Spring. */
class MeseroReglasTest {
    // ---- RF01 / RF02: cantidades y productos ----
    @Test void cantidadEntreUnoYElMaximo() {
        ReglasMesero.validarCantidad(1);
        ReglasMesero.validarCantidad(ReglasMesero.CANTIDAD_MAXIMA);
        for (Integer mala : new Integer[] { null, 0, -3, ReglasMesero.CANTIDAD_MAXIMA + 1 })
            assertThrows(ReglaNegocioException.class, () -> ReglasMesero.validarCantidad(mala));
    }
    @Test void productoSoloSiEstaActivoDisponibleYConExistencias() {
        ReglasMesero.validarProductoDisponible("Pizza", true, true, 3);
        assertThrows(ReglaNegocioException.class, () -> ReglasMesero.validarProductoDisponible("Pizza", false, true, 3));
        assertThrows(ReglaNegocioException.class, () -> ReglasMesero.validarProductoDisponible("Pizza", true, false, 3));
        assertThrows(ReglaNegocioException.class, () -> ReglasMesero.validarProductoDisponible("Pizza", true, true, 0));
        var e = assertThrows(ReglaNegocioException.class, () -> ReglasMesero.validarProductoDisponible("Pizza", true, true, null));
        assertTrue(e.getMessage().contains("Pizza"));
    }
    @Test void soloSeEditaUnPedidoPendiente() {
        ReglasMesero.exigirEditable(PENDIENTE);
        for (EstadoPedido e : new EstadoPedido[] { EN_PREPARACION, LISTO, ENTREGADO, CANCELADO, PAGADO })
            assertThrows(ReglaNegocioException.class, () -> ReglasMesero.exigirEditable(e));
    }

    // ---- RF03: entregar ----
    @Test void entregarSoloDesdeListo() {
        ReglasMesero.validarEntrega(LISTO);
        for (EstadoPedido e : new EstadoPedido[] { PENDIENTE, EN_PREPARACION, ENTREGADO, CANCELADO, PAGADO })
            assertThrows(ReglaNegocioException.class, () -> ReglasMesero.validarEntrega(e));
        assertTrue(assertThrows(ReglaNegocioException.class, () -> ReglasMesero.validarEntrega(PENDIENTE)).getMessage().contains("LISTO"));
    }

    // ---- RF04: cancelar ----
    @Test void cancelarSoloPendienteOEnPreparacion() {
        ReglasMesero.validarCancelacion(PENDIENTE);
        ReglasMesero.validarCancelacion(EN_PREPARACION);
        for (EstadoPedido e : new EstadoPedido[] { LISTO, ENTREGADO, CANCELADO, PAGADO })
            assertThrows(ReglaNegocioException.class, () -> ReglasMesero.validarCancelacion(e));
    }
    @Test void mensajeClaroParaEntregadoYPagado() {
        assertTrue(assertThrows(ReglaNegocioException.class, () -> ReglasMesero.validarCancelacion(ENTREGADO)).getMessage().contains("entregado"));
        assertTrue(assertThrows(ReglaNegocioException.class, () -> ReglasMesero.validarCancelacion(PAGADO)).getMessage().contains("pago"));
    }
    @Test void soloPendienteDevuelveExistencias() {
        assertTrue(ReglasMesero.devuelveExistencias(PENDIENTE));
        assertFalse(ReglasMesero.devuelveExistencias(EN_PREPARACION));
    }

    // ---- RF06: mesas ----
    @Test void cuentaAbiertaHastaPagarOCancelar() {
        for (EstadoPedido e : new EstadoPedido[] { PENDIENTE, EN_PREPARACION, LISTO, ENTREGADO }) assertTrue(ReglasMesero.cuentaAbierta(e));
        assertFalse(ReglasMesero.cuentaAbierta(PAGADO));
        assertFalse(ReglasMesero.cuentaAbierta(CANCELADO));
        assertFalse(ReglasMesero.cuentaAbierta(null));
    }
    @Test void mesaNoCambiaConCuentasAbiertasNiDesactivada() {
        ReglasMesero.validarCambioMesa(true, 0);
        assertThrows(ReglaNegocioException.class, () -> ReglasMesero.validarCambioMesa(true, 1));
        assertThrows(ReglaNegocioException.class, () -> ReglasMesero.validarCambioMesa(false, 0));
    }
    @Test void pedidoSoloEnMesaActivaYSinCuenta() {
        ReglasMesero.validarMesaParaPedido(true, 0);
        assertThrows(ReglaNegocioException.class, () -> ReglasMesero.validarMesaParaPedido(true, 1));
        assertThrows(ReglaNegocioException.class, () -> ReglasMesero.validarMesaParaPedido(false, 0));
    }
}
