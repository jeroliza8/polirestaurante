package com.polirestaurante.service;

import com.polirestaurante.entity.Promocion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** RNF17: totales, descuentos e impuestos probados de forma automatica. */
class CalculadoraCuentaTest {

    private static final List<CalculadoraCuenta.Linea> CUENTA = List.of(
            new CalculadoraCuenta.Linea(2, 18000, 2, false),   // 2 hamburguesas = 36.000
            new CalculadoraCuenta.Linea(1, 7000, 2, false),    // 2 papas        = 14.000
            new CalculadoraCuenta.Linea(4, 4500, 2, true));    // gaseosas ANULADAS (no suman)

    private Promocion promo(double pct, Integer categoria) {
        Promocion p = new Promocion();
        p.setCodigo("T");
        p.setNombre("Test");
        p.setPorcentaje(pct);
        p.setCategoriaId(categoria);
        return p;
    }

    @Test
    void sinPromocionLosAnuladosNoSuman() {
        CalculadoraCuenta.Totales t = CalculadoraCuenta.calcular(CUENTA, null, 8);
        assertEquals(50000, t.subtotal());
        assertEquals(0, t.descuento());
        assertEquals(50000, t.total());
        assertEquals(3703.7, t.impuesto());        // 8 % incluido: 50000 - 50000/1.08
    }

    @Test
    void promocionGeneral() {
        CalculadoraCuenta.Totales t = CalculadoraCuenta.calcular(CUENTA, promo(10, null), 8);
        assertEquals(5000, t.descuento());
        assertEquals(45000, t.total());
    }

    @Test
    void promocionPorCategoriaSoloDescuentaEsaCategoria() {
        CalculadoraCuenta.Totales t = CalculadoraCuenta.calcular(CUENTA, promo(15, 2), 8);
        assertEquals(5400, t.descuento());          // 15 % de 36.000
        assertEquals(44600, t.total());
    }

    @Test
    void promocionNoAplicaSiNoHayProductosDeLaCategoria() {
        assertFalse(CalculadoraCuenta.promocionAplica(CUENTA, promo(20, 5)));
        assertTrue(CalculadoraCuenta.promocionAplica(CUENTA, promo(15, 2)));
        // la categoria 4 solo tiene productos anulados
        assertFalse(CalculadoraCuenta.promocionAplica(CUENTA, promo(10, 4)));
    }

    @Test
    void sinImpuestoConfigurado() {
        assertEquals(0, CalculadoraCuenta.calcular(CUENTA, null, 0).impuesto());
    }
}
