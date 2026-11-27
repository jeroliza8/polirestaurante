package com.polirestaurante.dto;

import com.polirestaurante.enums.EstadoPedido;
import java.time.LocalDateTime;
import java.util.List;

/** DTOs de presentación del módulo Mesero; no son entidades JPA ni crean tablas. */
public final class MeseroDTO {
    private MeseroDTO() {}

    /** pedidoId es la cuenta abierta de la mesa (si la hay); cuentaPropia indica si es de quien consulta. */
    public record Mesa(Integer id, Integer numero, Integer capacidad, String estado,
                       Integer pedidoId, boolean cuentaPropia) {}
    public record Producto(Integer id, String nombre, String descripcion, String categoria,
                           Double precio, Integer existencias) {}
    public record Linea(Integer id, Integer productoId, String nombre, Integer cantidad, Double precioUnitario,
                        Double subtotal, String notas, boolean anulado, String motivoAnulacion) {}
    public record Pedido(Integer id, EstadoPedido estado, Integer mesaId, Integer mesaNumero,
                         LocalDateTime fechaCreacion, LocalDateTime fechaUltimaActualizacion, Double total,
                         boolean editable, boolean entregable, boolean cancelable, List<Linea> lineas) {}

    public record LineaRequest(Integer productoId, Integer cantidad, String notas) {}
    public record NuevoPedido(Integer mesaId, List<LineaRequest> lineas) {}
    public record Cancelacion(String motivo) {}
}
