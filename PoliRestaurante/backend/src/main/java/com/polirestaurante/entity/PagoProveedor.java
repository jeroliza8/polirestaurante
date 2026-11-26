package com.polirestaurante.entity;

import com.polirestaurante.enums.EstadoPagoProveedor;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "PagoProveedor")
@Getter @Setter @NoArgsConstructor
public class PagoProveedor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idPagoProveedor") private Integer idPagoProveedor;
    @Column(name = "monto") private Double monto;
    /** Fecha en que se genero la obligacion de pago (al recibir la mercancia). */
    @Column(name = "fecha") private LocalDateTime fecha;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado") private EstadoPagoProveedor estado;
    @Column(name = "PedidoInsumo_id_pedido_insumo", nullable = false) private Integer pedidoInsumoId;
    @Column(name = "fecha_pago") private LocalDateTime fechaPago;
}
