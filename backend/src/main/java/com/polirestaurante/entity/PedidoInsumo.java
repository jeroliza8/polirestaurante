package com.polirestaurante.entity;

import com.polirestaurante.enums.EstadoSolicitud;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Solicitud de insumos que el administrador envia a un proveedor (RF35). */
@Entity
@Table(name = "PedidoInsumo")
@Getter @Setter @NoArgsConstructor
public class PedidoInsumo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido_insumo") private Integer idPedidoInsumo;
    @Column(name = "fecha") private LocalDateTime fecha;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado") private EstadoSolicitud estado;
    @Column(name = "Administrador_id_administrador", nullable = false) private Integer administradorId;
    @Column(name = "Proveedor_id_proveedor") private Integer proveedorId;
    @Column(name = "fecha_recepcion") private LocalDateTime fechaRecepcion;
}
