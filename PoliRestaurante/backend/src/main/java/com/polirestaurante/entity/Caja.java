package com.polirestaurante.entity;

import com.polirestaurante.enums.EstadoCaja;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "Caja")
@Getter @Setter @NoArgsConstructor
public class Caja {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_caja") private Integer idCaja;
    @Column(name = "fecha_apertura") private LocalDateTime fechaApertura;
    @Column(name = "fecha_cierre") private LocalDateTime fechaCierre;
    @Column(name = "monto_inicial") private Double montoInicial;
    @Column(name = "monto_final") private Double montoFinal;
    @Column(name = "Cajero_id_cajero", nullable = false) private Integer cajeroId;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false) private EstadoCaja estado = EstadoCaja.CERRADA;
    @Column(name = "total_ventas") private Double totalVentas;
}
