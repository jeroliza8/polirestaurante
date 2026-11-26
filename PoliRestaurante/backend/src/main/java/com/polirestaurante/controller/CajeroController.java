package com.polirestaurante.controller;

import com.polirestaurante.dto.*;
import com.polirestaurante.enums.MedioPago;
import com.polirestaurante.security.UsuarioSesion;
import com.polirestaurante.service.CajeroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/** CajeroController del diagrama de componentes -> CajeroService. Solo rol CAJERO. */
@RestController
@RequestMapping("/api/cajero")
@RequiredArgsConstructor
public class CajeroController {

    private final CajeroService cajeroService;

    // ---------------- Caja (RF11) ----------------

    @GetMapping("/caja")
    public ResponseEntity<ResumenCajaDTO> cajaActual(@AuthenticationPrincipal UsuarioSesion sesion) {
        return cajeroService.consultarCajaActual(sesion)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/caja/apertura")
    @ResponseStatus(HttpStatus.CREATED)
    public ResumenCajaDTO abrirCaja(@Valid @RequestBody MontoRequest req, @AuthenticationPrincipal UsuarioSesion sesion) {
        return cajeroService.abrirCaja(sesion, req.monto());
    }

    @PostMapping("/caja/cierre")
    public ResumenCajaDTO cerrarCaja(@Valid @RequestBody MontoRequest req, @AuthenticationPrincipal UsuarioSesion sesion) {
        return cajeroService.cerrarCaja(sesion, req.monto());
    }

    @GetMapping("/caja/historial")
    public List<ResumenCajaDTO> historialCajas(@AuthenticationPrincipal UsuarioSesion sesion) {
        return cajeroService.historialCajas(sesion);
    }

    // ---------------- Cuentas (RF12-RF15, RF17) ----------------

    @GetMapping("/cuentas")
    public List<CuentaResumenDTO> cuentas() {
        return cajeroService.consultarCuentas();
    }

    @GetMapping("/cuentas/{pedidoId}")
    public CuentaDetalleDTO detalleCuenta(@PathVariable Integer pedidoId) {
        return cajeroService.consultarDetalleCuenta(pedidoId);
    }

    @GetMapping("/cuentas/{pedidoId}/entregados")
    public CuentaDetalleDTO comprobanteEntregados(@PathVariable Integer pedidoId) {
        return cajeroService.comprobanteEntregados(pedidoId);
    }

    @PostMapping("/cuentas/{pedidoId}/productos/{detalleId}/anular")
    public CuentaDetalleDTO anularProducto(@PathVariable Integer pedidoId, @PathVariable Integer detalleId,
                                           @Valid @RequestBody MotivoRequest req,
                                           @AuthenticationPrincipal UsuarioSesion sesion) {
        return cajeroService.anularProducto(sesion, pedidoId, detalleId, req.motivo());
    }

    @PostMapping("/cuentas/{pedidoId}/cancelar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelarCuenta(@PathVariable Integer pedidoId, @Valid @RequestBody MotivoRequest req,
                               @AuthenticationPrincipal UsuarioSesion sesion) {
        cajeroService.cancelarCuenta(sesion, pedidoId, req.motivo());
    }

    // ---------------- Promociones (RF22, RF23, CU-CAJ-002) ----------------

    @GetMapping("/promociones")
    public List<PromocionDTO> promociones(@RequestParam(required = false) Integer pedidoId) {
        return cajeroService.consultarPromociones(pedidoId);
    }

    @PostMapping("/cuentas/{pedidoId}/promocion")
    public CuentaDetalleDTO aplicarPromocion(@PathVariable Integer pedidoId, @Valid @RequestBody AplicarPromocionRequest req,
                                             @AuthenticationPrincipal UsuarioSesion sesion) {
        return cajeroService.aplicarPromocion(sesion, pedidoId, req.codigo());
    }

    @DeleteMapping("/cuentas/{pedidoId}/promocion")
    public CuentaDetalleDTO quitarPromocion(@PathVariable Integer pedidoId, @AuthenticationPrincipal UsuarioSesion sesion) {
        return cajeroService.quitarPromocion(sesion, pedidoId);
    }

    // ---------------- Pagos y facturas (RF16, RF18-RF21) ----------------

    @PostMapping("/pagos")
    @ResponseStatus(HttpStatus.CREATED)
    public PagoResultadoDTO registrarPago(@Valid @RequestBody PagoRequest req, @AuthenticationPrincipal UsuarioSesion sesion) {
        return cajeroService.registrarPago(sesion, req);
    }

    @PostMapping("/pagos/{pagoId}/factura")
    public FacturaDTO generarFactura(@PathVariable Integer pagoId, @Valid @RequestBody FacturaRequest req,
                                     @AuthenticationPrincipal UsuarioSesion sesion) {
        return cajeroService.generarFactura(sesion, pagoId, req);
    }

    @PostMapping("/facturas/{pagoId}/reintentar-electronica")
    public FacturaDTO reintentarElectronica(@PathVariable Integer pagoId, @AuthenticationPrincipal UsuarioSesion sesion) {
        return cajeroService.reintentarFacturaElectronica(sesion, pagoId);
    }

    @GetMapping("/facturas")
    public List<FacturaResumenDTO> historialFacturas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(required = false) MedioPago medioPago) {
        return cajeroService.historialFacturas(desde, hasta, medioPago);
    }

    @GetMapping("/facturas/{pagoId}")
    public FacturaDTO reimprimirFactura(@PathVariable Integer pagoId) {
        return cajeroService.reimprimirFactura(pagoId);
    }
}
