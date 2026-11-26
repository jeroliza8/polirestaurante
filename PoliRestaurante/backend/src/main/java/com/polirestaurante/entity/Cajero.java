package com.polirestaurante.entity;

import com.polirestaurante.enums.Rol;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Cajero")
@Getter @Setter @NoArgsConstructor
public class Cajero implements Empleado {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cajero") private Integer idCajero;
    @Column(name = "Usuario_idUsuario", nullable = false) private Integer usuarioId;

    @Override public Rol getRol() { return Rol.CAJERO; }
}
