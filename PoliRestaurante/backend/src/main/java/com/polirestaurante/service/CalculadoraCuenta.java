package com.polirestaurante.service;

import com.polirestaurante.entity.Promocion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Logica pura de totales de una cuenta (subtotal, descuento, impuesto incluido).
 * Sin dependencias de Spring ni de la base, para probarla con JUnit (RNF17).
 */
public final class CalculadoraCuenta {

    private CalculadoraCuenta() {
    }

    /** Un producto de la cuenta. Las lineas anuladas no suman. */
    public record Linea(Integer categoriaId, double precioUnitario, int cantidad, boolean anulada) {
        public double subtotal() {
            return redondear(precioUnitario * cantidad);
        }
    }

    public record Totales(double subtotal, double descuento, double total, double impuesto) {
    }

    /**
     * @param promocion          puede ser null
     * @param impuestoPorcentaje impuesto INCLUIDO en los precios (p. ej. impoconsumo 8 %)
     */
    public static Totales calcular(List<Linea> lineas, Promocion promocion, double impuestoPorcentaje) {
        double subtotal = redondear(lineas.stream().filter(l -> !l.anulada()).mapToDouble(Linea::subtotal).sum());
        double descuento = 0;
        if (promocion != null) {
            double base = promocion.getCategoriaId() == null
                    ? subtotal
                    : lineas.stream()
                        .filter(l -> !l.anulada() && promocion.getCategoriaId().equals(l.categoriaId()))
                        .mapToDouble(Linea::subtotal).sum();
            descuento = redondear(base * promocion.getPorcentaje() / 100.0);
        }
        double total = redondear(Math.max(0, subtotal - descuento));
        double impuesto = impuestoPorcentaje <= 0 ? 0
                : redondear(total - total / (1 + impuestoPorcentaje / 100.0));
        return new Totales(subtotal, descuento, total, impuesto);
    }

    /** Si la promocion aplica a una categoria, la cuenta debe tener productos de esa categoria. */
    public static boolean promocionAplica(List<Linea> lineas, Promocion promocion) {
        if (promocion.getCategoriaId() == null) {
            return lineas.stream().anyMatch(l -> !l.anulada());
        }
        return lineas.stream().anyMatch(l -> !l.anulada() && promocion.getCategoriaId().equals(l.categoriaId()));
    }

    public static double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
