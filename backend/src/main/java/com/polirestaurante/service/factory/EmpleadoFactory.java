package com.polirestaurante.service.factory;

import com.polirestaurante.dto.DatosRol;
import com.polirestaurante.entity.*;
import com.polirestaurante.enums.Rol;
import com.polirestaurante.exception.ReglaNegocioException;
import org.springframework.stereotype.Component;

/**
 * Patron Factory (seccion 15.4, modulo Administracion): crea la entidad del rol
 * correcto con sus atributos propios. Agregar un rol nuevo solo toca esta clase.
 */
@Component
public class EmpleadoFactory {

    public Empleado crear(Rol rol, Integer usuarioId, DatosRol datos) {
        DatosRol d = datos == null ? new DatosRol(null, null, null) : datos;
        Empleado empleado = switch (rol) {
            case ADMINISTRADOR -> new Administrador();
            case CAJERO -> new Cajero();
            case MESERO -> {
                Mesero m = new Mesero();
                m.setZonaAsignada(limpiar(d.zonaAsignada()));
                yield m;
            }
            case COCINERO -> {
                Cocinero c = new Cocinero();
                c.setEstacion(limpiar(d.estacion()));
                yield c;
            }
            case REPARTIDOR -> {
                Repartidor r = new Repartidor();
                r.setPlacaVehiculo(limpiar(d.placaVehiculo()) == null ? null : d.placaVehiculo().trim().toUpperCase());
                yield r;
            }
            case PROVEEDOR -> throw new ReglaNegocioException(
                    "Los proveedores se registran desde el módulo de Proveedores (CU-ADM-09)");
        };
        empleado.setUsuarioId(usuarioId);
        return empleado;
    }

    private String limpiar(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
