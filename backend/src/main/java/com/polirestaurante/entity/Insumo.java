package com.polirestaurante.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Insumo")
@Getter @Setter @NoArgsConstructor
public class Insumo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_insumo") private Integer idInsumo;
    @Column(name = "nombre") private String nombre;
    @Column(name = "cantidad_disponible") private Integer cantidadDisponible = 0;
    @Column(name = "unidad_medida", nullable = false) private String unidadMedida = "unidad";
    @Column(name = "stock_minimo", nullable = false) private Integer stockMinimo = 5;
}
