package com.polirestaurante.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Producto")
@Getter @Setter @NoArgsConstructor
public class Producto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idProducto") private Integer idProducto;
    @Column(name = "nombre") private String nombre;
    @Column(name = "descripcion") private String descripcion;
    @Column(name = "precio") private Double precio;
    @Column(name = "cantidad_disponible") private Integer cantidadDisponible;
    @Column(name = "Categoria_id_categoria", nullable = false) private Integer categoriaId;
    @Column(name = "disponible", nullable = false) private Boolean disponible = true;
    @Column(name = "activo", nullable = false) private Boolean activo = true;
    @Column(name = "stock_minimo", nullable = false) private Integer stockMinimo = 5;
}
