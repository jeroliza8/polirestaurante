package com.polirestaurante.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Que insumos suministra cada proveedor y a que precio (RF32). */
@Entity
@Table(name = "InsumoProveedor")
@Getter @Setter @NoArgsConstructor
public class InsumoProveedor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_insumo_proveedor") private Integer idInsumoProveedor;
    @Column(name = "precio") private Double precio;
    @Column(name = "Proveedor_id_proveedor1", nullable = false) private Integer proveedorId;
    @Column(name = "Insumo_id_insumo1", nullable = false) private Integer insumoId;
    @Column(name = "disponible", nullable = false) private Boolean disponible = true;
}
