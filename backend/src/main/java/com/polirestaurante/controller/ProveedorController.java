package com.polirestaurante.controller;

import com.polirestaurante.dto.*;
import com.polirestaurante.security.UsuarioSesion;
import com.polirestaurante.service.ProveedorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Portal del proveedor (RF34, RF35, RF36). Solo rol PROVEEDOR y solo ve lo suyo. */
@RestController
@RequestMapping("/api/proveedor")
@RequiredArgsConstructor
public class ProveedorController {

    private final ProveedorService proveedorService;

    @GetMapping("/perfil")
    public ProveedorDTO perfil(@AuthenticationPrincipal UsuarioSesion sesion) {
        return proveedorService.perfil(sesion);
    }

    @GetMapping("/solicitudes")
    public List<SolicitudDTO> solicitudes(@AuthenticationPrincipal UsuarioSesion sesion) {
        return proveedorService.solicitudesRecibidas(sesion);
    }

    @PostMapping("/solicitudes/{id}/despachar")
    public SolicitudDTO despachar(@PathVariable Integer id, @AuthenticationPrincipal UsuarioSesion sesion) {
        return proveedorService.despacharSolicitud(sesion, id);
    }

    @GetMapping("/entregas")
    public List<SolicitudDTO> entregas(@AuthenticationPrincipal UsuarioSesion sesion) {
        return proveedorService.historialEntregas(sesion);
    }

    @GetMapping("/pagos")
    public ResumenPagosProveedorDTO pagos(@AuthenticationPrincipal UsuarioSesion sesion) {
        return proveedorService.pagosDelProveedor(sesion);
    }

    @GetMapping("/catalogo")
    public List<InsumoSuministradoDTO> catalogo(@AuthenticationPrincipal UsuarioSesion sesion) {
        return proveedorService.catalogo(sesion);
    }

    @PutMapping("/catalogo/{relacionId}")
    public InsumoSuministradoDTO actualizarCatalogo(@PathVariable Integer relacionId, @Valid @RequestBody CatalogoRequest req,
                                                    @AuthenticationPrincipal UsuarioSesion sesion) {
        return proveedorService.actualizarCatalogo(sesion, relacionId, req);
    }
}
