package com.polirestaurante.entity;

import com.polirestaurante.enums.EstadoDetalle;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "DetallePedido")
@Getter @Setter @NoArgsConstructor
public class DetallePedido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_pedido") private Integer idDetallePedido;
    @Column(name = "cantidad") private Integer cantidad;
    @Column(name = "notas", columnDefinition = "TEXT") private String notas;
    @Column(name = "Producto_idProducto", nullable = false) private Integer productoId;
    @Column(name = "Pedido_id_pedido", nullable = false) private Integer pedidoId;
    /** Precio con el que se cobro; si es null se usa el precio actual del producto. */
    @Column(name = "precio_unitario") private Double precioUnitario;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false) private EstadoDetalle estado = EstadoDetalle.PENDIENTE;
    @Column(name = "fecha_anulacion") private LocalDateTime fechaAnulacion;
    @Column(name = "motivo_anulacion") private String motivoAnulacion;
    @Column(name = "Usuario_anulacion") private Integer usuarioAnulacion;
}
