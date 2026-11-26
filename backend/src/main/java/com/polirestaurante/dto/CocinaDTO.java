package com.polirestaurante.dto;

import com.polirestaurante.enums.EstadoPedido;
import java.time.LocalDateTime;
import java.util.List;

/** DTOs de presentación; no son entidades JPA ni crean tablas. */
public final class CocinaDTO {
    private CocinaDTO() {}
    public record Item(Integer id, Integer productoId, String nombre, Integer cantidad, String notas) {}
    public record Orden(Integer id, EstadoPedido estado, LocalDateTime fechaCreacion,
                       LocalDateTime fechaUltimaActualizacion, String tipoPedido, String mesero,
                       List<Integer> mesas, LocalDateTime fechaEnPreparacion, LocalDateTime fechaListo,
                       Integer usuarioPreparacion, Integer usuarioListo, List<Item> items) {}
    public record Producto(Integer id, String nombre, String categoria, Integer cantidadDisponible,
                           boolean disponible, boolean disponibilidadEfectiva,
                           LocalDateTime fechaDisponibilidad, Integer usuarioDisponibilidad) {}
    public record Tablero(List<Orden> pedidos, List<Producto> productos) {}
    public record CambioEstado(EstadoPedido estado) {}
    public record CambioDisponibilidad(Boolean disponible) {}
}
