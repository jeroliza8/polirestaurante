package com.polirestaurante.service.factory;

import com.polirestaurante.dto.DatosRol;
import com.polirestaurante.entity.*;
import com.polirestaurante.enums.Rol;
import com.polirestaurante.exception.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Patron Factory: cada rol crea su entidad con sus atributos propios. */
class EmpleadoFactoryTest {

    private final EmpleadoFactory factory = new EmpleadoFactory();

    @Test
    void creaCadaRolConSusDatos() {
        DatosRol datos = new DatosRol("Parrilla", "Terraza", "abc12d");
        assertInstanceOf(Administrador.class, factory.crear(Rol.ADMINISTRADOR, 1, datos));
        assertInstanceOf(Cajero.class, factory.crear(Rol.CAJERO, 1, datos));
        assertEquals("Terraza", ((Mesero) factory.crear(Rol.MESERO, 1, datos)).getZonaAsignada());
        assertEquals("Parrilla", ((Cocinero) factory.crear(Rol.COCINERO, 1, datos)).getEstacion());
        assertEquals("ABC12D", ((Repartidor) factory.crear(Rol.REPARTIDOR, 1, datos)).getPlacaVehiculo());
        assertEquals(7, factory.crear(Rol.CAJERO, 7, null).getUsuarioId());
    }

    @Test
    void proveedorNoSeCreaComoEmpleado() {
        assertThrows(ReglaNegocioException.class, () -> factory.crear(Rol.PROVEEDOR, 1, null));
    }
}
