package com.polirestaurante.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Promocion o descuento (RF22, RF23, CU-CAJ-002).
 * NO es una entidad JPA: el modelo relacional no tiene tabla de promociones y no
 * se crean tablas nuevas, por eso el catalogo se define en application.yml
 * (polirestaurante.promociones) y lo lee {@code PromocionDAO}.
 */
@Getter @Setter @NoArgsConstructor
public class Promocion {
    private String codigo;
    private String nombre;
    private String descripcion;
    /** Porcentaje de descuento, de 1 a 100. */
    private double porcentaje;
    /** Si se indica, el descuento solo aplica a los productos de esa categoria. */
    private Integer categoriaId;
    /** Fechas en formato ISO yyyy-MM-dd (texto, para que la lectura del YAML no dependa del idioma). */
    private String fechaInicio;
    private String fechaFin;
    /** Dias en que aplica (vacio = todos). */
    private List<DayOfWeek> diasSemana = new ArrayList<>();
    private boolean activa = true;

    public boolean vigente(LocalDateTime momento) {
        LocalDate dia = momento.toLocalDate();
        LocalDate inicio = inicio();
        LocalDate fin = fin();
        return activa
                && (inicio == null || !dia.isBefore(inicio))
                && (fin == null || !dia.isAfter(fin))
                && (diasSemana == null || diasSemana.isEmpty() || diasSemana.contains(dia.getDayOfWeek()));
    }

    public LocalDate inicio() {
        return fechaInicio == null || fechaInicio.isBlank() ? null : LocalDate.parse(fechaInicio.trim());
    }

    public LocalDate fin() {
        return fechaFin == null || fechaFin.isBlank() ? null : LocalDate.parse(fechaFin.trim());
    }
}
