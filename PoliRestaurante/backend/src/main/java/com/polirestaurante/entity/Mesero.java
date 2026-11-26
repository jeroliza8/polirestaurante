package com.polirestaurante.entity;

import com.polirestaurante.enums.Rol;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Mesero")
@Getter @Setter @NoArgsConstructor
public class Mesero implements Empleado {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mesero") private Integer idMesero;
    @Column(name = "zona_asignada") private String zonaAsignada;
    @Column(name = "Usuario_idUsuario", nullable = false) private Integer usuarioId;

    @Override public Rol getRol() { return Rol.MESERO; }
}
