package com.polirestaurante.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "PedidoMesa")
@Getter @Setter @NoArgsConstructor
public class PedidoMesa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido_mesa") private Integer idPedidoMesa;
    @Column(name = "Mesa_id_mesa", nullable = false) private Integer mesaId;
    @Column(name = "Pedido_id_pedido", nullable = false) private Integer pedidoId;
}
