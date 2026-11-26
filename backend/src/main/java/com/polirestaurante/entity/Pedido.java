package com.polirestaurante.entity;

import com.polirestaurante.enums.EstadoPedido;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Un pedido abierto es, para el cajero, una "cuenta" (RF12-RF17). */
@Entity
@Table(name = "Pedido")
@Getter @Setter @NoArgsConstructor
public class Pedido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido") private Integer idPedido;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado") private EstadoPedido estado;
    @Column(name = "fecha_creacion") private LocalDateTime fechaCreacion;
    @Column(name = "fecha_ultima_actualizacion") private LocalDateTime fechaUltimaActualizacion;
    @Column(name = "tipo_pedido") private String tipoPedido;
    @Column(name = "Mesero_id_mesero", nullable = false) private Integer meseroId;
    @Column(name = "Repartidor_id_repartidor") private Integer repartidorId;
    @Column(name = "Cliente_id_cliente") private Integer clienteId;
    @Column(name = "fecha_en_preparacion") private LocalDateTime fechaEnPreparacion;
    @Column(name = "fecha_listo") private LocalDateTime fechaListo;
    @Column(name = "fecha_entregado") private LocalDateTime fechaEntregado;
    @Column(name = "promocion_aplicada") private String promocionAplicada;
}
