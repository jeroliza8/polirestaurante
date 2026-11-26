package com.polirestaurante.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "DetallePedidoInsumo")
@Getter @Setter @NoArgsConstructor
public class DetallePedidoInsumo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_pedido_insumo") private Integer idDetallePedidoInsumo;
    @Column(name = "cantidad") private Integer cantidad;
    @Column(name = "Insumo_id_insumo1", nullable = false) private Integer insumoId;
    @Column(name = "PedidoInsumo_id_pedido_insumo", nullable = false) private Integer pedidoInsumoId;
    @Column(name = "precio_unitario") private Double precioUnitario;
}
