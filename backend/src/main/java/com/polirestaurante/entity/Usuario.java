package com.polirestaurante.entity;

import com.polirestaurante.enums.EstadoUsuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Usuario")
@Getter @Setter @NoArgsConstructor
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idUsuario") private Integer idUsuario;
    @Column(name = "nombre") private String nombre;
    /** Hash BCrypt de la contrasena (RNF05). Nunca se envia al frontend. */
    @Column(name = "credenciales") private String credenciales;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false) private EstadoUsuario estado = EstadoUsuario.activo;
}
