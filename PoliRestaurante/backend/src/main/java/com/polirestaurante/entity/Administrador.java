package com.polirestaurante.entity;

import com.polirestaurante.enums.Rol;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Administrador")
@Getter @Setter @NoArgsConstructor
public class Administrador implements Empleado {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_administrador") private Integer idAdministrador;
    @Column(name = "Usuario_idUsuario", nullable = false) private Integer usuarioId;

    @Override public Rol getRol() { return Rol.ADMINISTRADOR; }
}
