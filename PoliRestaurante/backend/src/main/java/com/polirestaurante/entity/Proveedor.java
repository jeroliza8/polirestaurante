package com.polirestaurante.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Proveedor")
@Getter @Setter @NoArgsConstructor
public class Proveedor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proveedor") private Integer idProveedor;
    @Column(name = "nombre") private String nombre;
    @Column(name = "contacto") private String contacto;
    @Column(name = "Usuario_idUsuario") private Integer usuarioId;
}
