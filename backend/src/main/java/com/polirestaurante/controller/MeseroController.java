package com.polirestaurante.controller;

import com.polirestaurante.dto.MeseroDTO;
import com.polirestaurante.security.UsuarioSesion;
import com.polirestaurante.service.MeseroService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Presentación REST del módulo de mesero. RBAC: exclusivamente MESERO (SecurityConfig). */
@RestController
@RequestMapping("/api/mesero")
public class MeseroController {
    private final MeseroService mesero;
    public MeseroController(MeseroService mesero) { this.mesero = mesero; }

    // ---- consultas ----
    @GetMapping("/mesas")
    public List<MeseroDTO.Mesa> mesas(@AuthenticationPrincipal UsuarioSesion s) { return mesero.consultarMesas(s); }

    @GetMapping("/menu")
    public List<MeseroDTO.Producto> menu(@AuthenticationPrincipal UsuarioSesion s) { return mesero.consultarMenu(s); }

    @GetMapping("/pedidos")
    public List<MeseroDTO.Pedido> misPedidos(@AuthenticationPrincipal UsuarioSesion s) { return mesero.consultarMisPedidos(s); }

    @GetMapping("/pedidos/{id}")
    public MeseroDTO.Pedido pedido(@PathVariable Integer id, @AuthenticationPrincipal UsuarioSesion s) {
        return mesero.consultarPedido(id, s);
    }

    // ---- mesas (RF06) ----
    @PatchMapping("/mesas/{id}/ocupar")
    public MeseroDTO.Mesa ocupar(@PathVariable Integer id, @AuthenticationPrincipal UsuarioSesion s) { return mesero.ocuparMesa(id, s); }

    @PatchMapping("/mesas/{id}/liberar")
    public MeseroDTO.Mesa liberar(@PathVariable Integer id, @AuthenticationPrincipal UsuarioSesion s) { return mesero.liberarMesa(id, s); }

    // ---- pedidos (RF01-RF04) ----
    @PostMapping("/pedidos")
    @ResponseStatus(HttpStatus.CREATED)
    public MeseroDTO.Pedido crear(@RequestBody MeseroDTO.NuevoPedido req, @AuthenticationPrincipal UsuarioSesion s) {
        return mesero.crearPedido(req, s);
    }

    @PostMapping("/pedidos/{id}/productos")
    public MeseroDTO.Pedido agregar(@PathVariable Integer id, @RequestBody MeseroDTO.LineaRequest req,
                                    @AuthenticationPrincipal UsuarioSesion s) {
        return mesero.agregarProducto(id, req, s);
    }

    @DeleteMapping("/pedidos/{id}/productos/{detalleId}")
    public MeseroDTO.Pedido quitar(@PathVariable Integer id, @PathVariable Integer detalleId,
                                   @AuthenticationPrincipal UsuarioSesion s) {
        return mesero.eliminarProducto(id, detalleId, s);
    }

    @PatchMapping("/pedidos/{id}/entregar")
    public MeseroDTO.Pedido entregar(@PathVariable Integer id, @AuthenticationPrincipal UsuarioSesion s) {
        return mesero.entregarPedido(id, s);
    }

    @PatchMapping("/pedidos/{id}/cancelar")
    public MeseroDTO.Pedido cancelar(@PathVariable Integer id, @RequestBody(required = false) MeseroDTO.Cancelacion req,
                                     @AuthenticationPrincipal UsuarioSesion s) {
        return mesero.cancelarPedido(id, req == null ? null : req.motivo(), s);
    }
}
