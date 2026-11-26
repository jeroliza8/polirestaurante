package com.polirestaurante.service;

import com.polirestaurante.enums.EstadoPedido;
import com.polirestaurante.exception.ConflictoException;
import com.polirestaurante.exception.ReglaNegocioException;
import com.polirestaurante.service.state.DisponibilidadCocina;
import com.polirestaurante.service.state.EstadoCocina;
import org.junit.jupiter.api.Test;
import static com.polirestaurante.enums.EstadoPedido.*;
import static org.junit.jupiter.api.Assertions.*;

class CocinaReglasTest {
    @Test void soloDosTransicionesAutorizadas() {
        for (EstadoPedido actual : EstadoPedido.values()) for (EstadoPedido destino : EstadoPedido.values()) {
            if ((actual == PENDIENTE && destino == EN_PREPARACION) || (actual == EN_PREPARACION && destino == LISTO))
                assertEquals(destino, EstadoCocina.validarTransicion(actual, destino));
            else assertThrows(ReglaNegocioException.class, () -> EstadoCocina.validarTransicion(actual, destino));
        }
    }
    @Test void noModificaCancelados() {
        var e = assertThrows(ReglaNegocioException.class, () -> EstadoCocina.validarTransicion(CANCELADO, EN_PREPARACION));
        assertTrue(e.getMessage().contains("cancelado"));
    }
    @Test void noSaltaPreparacion() {
        assertThrows(ReglaNegocioException.class, () -> EstadoCocina.validarTransicion(PENDIENTE, LISTO));
    }
    @Test void noAceptaEstadosNulos() {
        assertThrows(ReglaNegocioException.class, () -> EstadoCocina.validarTransicion(null, LISTO));
        assertThrows(ReglaNegocioException.class, () -> EstadoCocina.validarTransicion(PENDIENTE, null));
    }
    @Test void stateDelPedido() {
        assertEquals(EN_PREPARACION, EstadoCocina.de(PENDIENTE).avanzar());
        assertEquals(LISTO, EstadoCocina.de(EN_PREPARACION).avanzar());
        assertEquals(ENTREGADO, EstadoCocina.de(LISTO).avanzar());
        assertEquals(CANCELADO, EstadoCocina.de(PENDIENTE).cancelar());
        assertEquals(CANCELADO, EstadoCocina.de(EN_PREPARACION).cancelar());
        for (var finalizado : new EstadoPedido[] { ENTREGADO, CANCELADO, PAGADO }) {
            assertThrows(ReglaNegocioException.class, () -> EstadoCocina.de(finalizado).avanzar());
            assertThrows(ReglaNegocioException.class, () -> EstadoCocina.de(finalizado).cancelar());
        }
        assertThrows(ReglaNegocioException.class, () -> EstadoCocina.de(LISTO).cancelar());
    }
    @Test void marcarAgotadoNoExigeCantidadCero() { assertDoesNotThrow(() -> DisponibilidadCocina.validar(true, false, 7)); }
    @Test void reactivarConStock() { assertDoesNotThrow(() -> DisponibilidadCocina.validar(false, true, 1)); }
    @Test void reactivarSinStockSeRechaza() {
        for (Integer cantidad : new Integer[] { null, 0, -1 })
            assertThrows(ReglaNegocioException.class, () -> DisponibilidadCocina.validar(false, true, cantidad));
    }
    @Test void cambioRepetidoSeRechaza() {
        assertThrows(ConflictoException.class, () -> DisponibilidadCocina.validar(false, false, 4));
        assertThrows(ConflictoException.class, () -> DisponibilidadCocina.validar(true, true, 4));
    }
    @Test void disponibilidadEfectiva() {
        assertTrue(DisponibilidadCocina.efectiva(true, 1));
        assertFalse(DisponibilidadCocina.efectiva(true, 0));
        assertFalse(DisponibilidadCocina.efectiva(false, 8));
        assertFalse(DisponibilidadCocina.efectiva(true, null));
    }
}
