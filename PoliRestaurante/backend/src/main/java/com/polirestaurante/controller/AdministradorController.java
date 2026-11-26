package com.polirestaurante.controller;

import com.polirestaurante.dto.*;
import com.polirestaurante.security.UsuarioSesion;
import com.polirestaurante.service.ProductoService;
import com.polirestaurante.service.ProveedorService;
import com.polirestaurante.service.UsuarioService;
import com.polirestaurante.service.reporte.ExportadorReportes;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AdministradorController del diagrama de componentes. Usa las interfaces de
 * ProductoService, UsuarioService y ProveedorService. Solo rol ADMINISTRADOR.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdministradorController {

    private final ProductoService productoService;
    private final UsuarioService usuarioService;
    private final ProveedorService proveedorService;
    private final ExportadorReportes exportador;

    // ---------------- Menu: productos (HU-ADM-01, CU-ADM-04) ----------------

    @GetMapping("/productos")
    public List<ProductoDTO> productos() {
        return productoService.listarProductos();
    }

    @PostMapping("/productos")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoDTO crearProducto(@Valid @RequestBody ProductoRequest req) {
        return productoService.crearProducto(req);
    }

    @PutMapping("/productos/{id}")
    public ProductoDTO editarProducto(@PathVariable Integer id, @Valid @RequestBody ProductoRequest req) {
        return productoService.editarProducto(id, req);
    }

    @DeleteMapping("/productos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarProducto(@PathVariable Integer id) {
        productoService.eliminarProducto(id);
    }

    // ---------------- Menu: categorias (RF25, CU-ADM-05) ----------------

    @GetMapping("/categorias")
    public List<CategoriaDTO> categorias() {
        return productoService.listarCategorias();
    }

    @PostMapping("/categorias")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaDTO crearCategoria(@Valid @RequestBody CategoriaRequest req) {
        return productoService.crearCategoria(req.nombre());
    }

    @PutMapping("/categorias/{id}")
    public CategoriaDTO editarCategoria(@PathVariable Integer id, @Valid @RequestBody CategoriaRequest req) {
        return productoService.editarCategoria(id, req.nombre());
    }

    @DeleteMapping("/categorias/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarCategoria(@PathVariable Integer id) {
        productoService.eliminarCategoria(id);
    }

    // ---------------- Mesas (RF26, CU-ADM-02) ----------------

    @GetMapping("/mesas")
    public List<MesaDTO> mesas() {
        return usuarioService.listarMesas();
    }

    @PostMapping("/mesas")
    @ResponseStatus(HttpStatus.CREATED)
    public MesaDTO crearMesa(@Valid @RequestBody MesaRequest req) {
        return usuarioService.registrarMesa(req);
    }

    @PutMapping("/mesas/{id}")
    public MesaDTO editarMesa(@PathVariable Integer id, @Valid @RequestBody MesaRequest req) {
        return usuarioService.editarMesa(id, req);
    }

    @DeleteMapping("/mesas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarMesa(@PathVariable Integer id) {
        usuarioService.eliminarMesa(id);
    }

    // ---------------- Usuarios y roles (RF27, HU-ADM-02, CU-ADM-03) ----------------

    @GetMapping("/usuarios")
    public List<UsuarioDTO> usuarios() {
        return usuarioService.listarUsuarios();
    }

    @GetMapping("/roles")
    public List<RolDTO> roles() {
        return usuarioService.listarRoles();
    }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDTO crearUsuario(@Valid @RequestBody CrearUsuarioRequest req) {
        return usuarioService.registrarEmpleado(req);
    }

    @PutMapping("/usuarios/{id}/rol")
    public UsuarioDTO cambiarRol(@PathVariable Integer id, @Valid @RequestBody CambioRolRequest req,
                                 @AuthenticationPrincipal UsuarioSesion sesion) {
        return usuarioService.cambiarRol(id, req, sesion);
    }

    @PatchMapping("/usuarios/{id}/estado")
    public UsuarioDTO cambiarEstado(@PathVariable Integer id, @Valid @RequestBody EstadoUsuarioRequest req,
                                    @AuthenticationPrincipal UsuarioSesion sesion) {
        return usuarioService.cambiarEstadoUsuario(id, req.activo(), sesion);
    }

    @PutMapping("/usuarios/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restablecerPassword(@PathVariable Integer id, @Valid @RequestBody PasswordRequest req) {
        usuarioService.restablecerPassword(id, req.password());
    }

    // ---------------- Reportes (RF28, RF29, CU-ADM-01, CU-ADM-06) ----------------

    @GetMapping("/reportes/ventas")
    public ReporteVentasDTO reporteVentas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(required = false) Integer categoriaId) {
        return productoService.obtenerReporteVentas(desde, hasta, categoriaId);
    }

    @GetMapping("/reportes/ventas/exportar")
    public ResponseEntity<byte[]> exportarVentas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(required = false) Integer categoriaId,
            @RequestParam(defaultValue = "pdf") String formato) {
        ReporteVentasDTO r = productoService.obtenerReporteVentas(desde, hasta, categoriaId);
        boolean excel = "excel".equalsIgnoreCase(formato) || "xlsx".equalsIgnoreCase(formato);
        byte[] archivo = excel ? exportador.ventasExcel(r) : exportador.ventasPdf(r);
        String nombre = "reporte-ventas-" + desde.toLocalDate() + "_" + hasta.toLocalDate() + (excel ? ".xlsx" : ".pdf");
        MediaType tipo = excel
                ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                : MediaType.APPLICATION_PDF;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(tipo)
                .body(archivo);
    }

    @GetMapping("/reportes/tiempos")
    public ReporteTiemposDTO reporteTiempos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return productoService.generarReporteTiempos(desde, hasta);
    }

    @GetMapping("/reportes/agotados")
    public ReporteAgotadosDTO reporteAgotados() {
        return productoService.generarReporteAgotados();
    }

    // ---------------- Inventario (RF30, RF33, CU-ADM-07, CU-ADM-10) ----------------

    @GetMapping("/inventario")
    public InventarioDTO inventario() {
        return productoService.obtenerDisponibilidadInsumos();
    }

    @PostMapping("/insumos")
    @ResponseStatus(HttpStatus.CREATED)
    public InsumoDTO crearInsumo(@Valid @RequestBody InsumoRequest req) {
        return productoService.crearInsumo(req);
    }

    @PutMapping("/insumos/{id}")
    public InsumoDTO editarInsumo(@PathVariable Integer id, @Valid @RequestBody InsumoRequest req) {
        return productoService.editarInsumo(id, req);
    }

    @PostMapping("/insumos/{id}/ingresos")
    public InsumoDTO registrarIngreso(@PathVariable Integer id, @Valid @RequestBody IngresoMercanciaRequest req,
                                      @AuthenticationPrincipal UsuarioSesion sesion) {
        return productoService.registrarIngresoMercancia(id, req, sesion);
    }

    // ---------------- Proveedores (RF32, RF35, RF36, CU-ADM-09) ----------------

    @GetMapping("/proveedores")
    public List<ProveedorDTO> proveedores() {
        return proveedorService.listarProveedores();
    }

    @PostMapping("/proveedores")
    @ResponseStatus(HttpStatus.CREATED)
    public ProveedorDTO registrarProveedor(@Valid @RequestBody ProveedorRequest req) {
        return proveedorService.registrarProveedor(req);
    }

    @PutMapping("/proveedores/{id}")
    public ProveedorDTO editarProveedor(@PathVariable Integer id, @Valid @RequestBody EditarProveedorRequest req) {
        return proveedorService.editarProveedor(id, req);
    }

    @PostMapping("/proveedores/{id}/insumos")
    public ProveedorDTO vincularInsumo(@PathVariable Integer id, @Valid @RequestBody InsumoPrecio req) {
        return proveedorService.vincularInsumo(id, req);
    }

    @DeleteMapping("/proveedores/{id}/insumos/{relacionId}")
    public ProveedorDTO desvincularInsumo(@PathVariable Integer id, @PathVariable Integer relacionId) {
        return proveedorService.desvincularInsumo(id, relacionId);
    }

    @GetMapping("/solicitudes")
    public List<SolicitudDTO> solicitudes() {
        return proveedorService.listarSolicitudes();
    }

    @PostMapping("/solicitudes")
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudDTO crearSolicitud(@Valid @RequestBody SolicitudRequest req,
                                       @AuthenticationPrincipal UsuarioSesion sesion) {
        return proveedorService.crearSolicitud(sesion, req);
    }

    @PostMapping("/solicitudes/{id}/recibir")
    public SolicitudDTO recibirSolicitud(@PathVariable Integer id) {
        return proveedorService.recibirSolicitud(id);
    }

    @PostMapping("/solicitudes/{id}/cancelar")
    public SolicitudDTO cancelarSolicitud(@PathVariable Integer id) {
        return proveedorService.cancelarSolicitud(id);
    }

    @GetMapping("/pagos-proveedor")
    public List<PagoProveedorDTO> pagosProveedor() {
        return proveedorService.listarPagos();
    }

    @PostMapping("/pagos-proveedor/{id}/pagar")
    public PagoProveedorDTO pagarProveedor(@PathVariable Integer id) {
        return proveedorService.pagarProveedor(id);
    }
}
