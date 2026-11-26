package com.polirestaurante.entity;

import com.polirestaurante.enums.Rol;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Cocinero")
@Getter @Setter @NoArgsConstructor
public class Cocinero implements Empleado {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cocinero") private Integer idCocinero;
    @Column(name = "estacion") private String estacion;
    @Column(name = "Usuario_idUsuario", nullable = false) private Integer usuarioId;

    @Override public Rol getRol() { return Rol.COCINERO; }
}
