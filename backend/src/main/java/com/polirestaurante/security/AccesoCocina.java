package com.polirestaurante.security;

import com.polirestaurante.dao.*;
import com.polirestaurante.enums.EstadoUsuario;
import com.polirestaurante.enums.Rol;
import com.polirestaurante.exception.AutenticacionException;
import org.springframework.stereotype.Component;

/** Revalida el actor actual, incluso si cambió de rol tras emitir el JWT. */
@Component
public class AccesoCocina {
    private final UsuarioDAO usuarios;
    private final CocineroDAO cocineros;
    private final MeseroDAO meseros;
    private final AdministradorDAO administradores;
    public AccesoCocina(UsuarioDAO usuarios, CocineroDAO cocineros, MeseroDAO meseros, AdministradorDAO administradores) {
        this.usuarios = usuarios; this.cocineros = cocineros; this.meseros = meseros; this.administradores = administradores;
    }
    public boolean vigente(UsuarioSesion sesion) {
        if (sesion == null || sesion.id() == null || sesion.rol() == null) return false;
        if (!usuarios.findById(sesion.id()).map(u -> u.getEstado() == EstadoUsuario.activo).orElse(false)) return false;
        return switch (sesion.rol()) {
            case COCINERO -> cocineros.findByUsuarioId(sesion.id()).isPresent();
            case MESERO -> meseros.findByUsuarioId(sesion.id()).isPresent();
            case ADMINISTRADOR -> administradores.findByUsuarioId(sesion.id()).isPresent();
            default -> false;
        };
    }
    public void exigirCocinero(UsuarioSesion sesion) {
        if (sesion == null || sesion.rol() != Rol.COCINERO || !vigente(sesion))
            throw new AutenticacionException("La sesión no corresponde a un cocinero activo.");
    }
    public Integer meseroId(UsuarioSesion sesion) {
        return sesion.rol() == Rol.MESERO ? meseros.findByUsuarioId(sesion.id()).map(m -> m.getIdMesero()).orElse(null) : null;
    }
}
