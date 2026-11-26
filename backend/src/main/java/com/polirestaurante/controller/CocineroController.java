package com.polirestaurante.controller;

import com.polirestaurante.dto.CocinaDTO;
import com.polirestaurante.security.UsuarioSesion;
import com.polirestaurante.service.CocineroService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Presentación REST del módulo de cocina. RBAC: exclusivamente COCINERO. */
@RestController
@RequestMapping("/api/cocinero")
public class CocineroController {
    private final CocineroService cocina;
    public CocineroController(CocineroService cocina) { this.cocina = cocina; }

    @GetMapping("/tablero")
    public CocinaDTO.Tablero tablero(@AuthenticationPrincipal UsuarioSesion sesion) { return cocina.tablero(sesion); }

    @GetMapping("/pedidos/{id}")
    public CocinaDTO.Orden consultarDetallePedido(@PathVariable Integer id, @AuthenticationPrincipal UsuarioSesion sesion) {
        return cocina.consultarDetallePedido(id, sesion);
    }

    @PatchMapping("/pedidos/{id}/estado")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void actualizarEstadoPedido(@PathVariable Integer id, @RequestBody CocinaDTO.CambioEstado cambio,
                                        @AuthenticationPrincipal UsuarioSesion sesion) {
        cocina.actualizarEstadoPedido(id, cambio.estado(), sesion);
    }

    @PatchMapping("/productos/{id}/disponibilidad")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void actualizarDisponibilidad(@PathVariable Integer id, @RequestBody CocinaDTO.CambioDisponibilidad cambio,
                                         @AuthenticationPrincipal UsuarioSesion sesion) {
        cocina.actualizarDisponibilidad(id, cambio.disponible(), sesion);
    }
}
