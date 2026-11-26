package com.polirestaurante.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Mesa")
@Getter @Setter @NoArgsConstructor
public class Mesa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mesa") private Integer idMesa;
    @Column(name = "numero") private Integer numero;
    /** LIBRE / OCUPADA (lo administra el modulo del mesero, RF06). */
    @Column(name = "estado") private String estado;
    @Column(name = "capacidad", nullable = false) private Integer capacidad = 4;
    @Column(name = "activa", nullable = false) private Boolean activa = true;
}
