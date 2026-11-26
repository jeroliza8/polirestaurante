package com.polirestaurante.entity;

import com.polirestaurante.enums.Rol;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Repartidor")
@Getter @Setter @NoArgsConstructor
public class Repartidor implements Empleado {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_repartidor") private Integer idRepartidor;
    @Column(name = "placa_vehiculo") private String placaVehiculo;
    @Column(name = "Usuario_idUsuario", nullable = false) private Integer usuarioId;

    @Override public Rol getRol() { return Rol.REPARTIDOR; }
}
