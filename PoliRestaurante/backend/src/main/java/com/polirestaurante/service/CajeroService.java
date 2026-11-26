package com.polirestaurante.service;

import com.polirestaurante.config.PoliRestauranteProperties;
import com.polirestaurante.dao.*;
import com.polirestaurante.dto.*;
import com.polirestaurante.entity.*;
import com.polirestaurante.enums.*;
import com.polirestaurante.exception.*;
import com.polirestaurante.security.UsuarioSesion;
import com.polirestaurante.service.integracion.IntegracionException;
import com.polirestaurante.service.integracion.ProveedorFacturaElectronica;
import com.polirestaurante.service.pago.MedioPagoStrategy;
import com.polirestaurante.service.pago.ResultadoPago;
import com.polirestaurante.service.pago.SolicitudCobro;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Componente CajeroService del diagrama de componentes (modulo Cajero).
 * Caja (RF11), cuentas (RF12-RF15, RF17), promociones (RF22, RF23 / CU-CAJ-002),
 * pagos y facturas (RF16, RF18-RF21 / CU-CAJ-001, HU-CAJ-001, HU-CAJ-002).
 * Es el "contexto" del patron Strategy para los medios de pago.
 */
@Slf4j
@Service
public class CajeroService {

    private final CajeroDAO cajeroDAO;
    private final CajaDAO cajaDAO;
    private final PedidoDAO pedidoDAO;
    private final PedidoMesaDAO pedidoMesaDAO;
    private final MesaDAO mesaDAO;
    private final MeseroDAO meseroDAO;
    private final UsuarioDAO usuarioDAO;
    private final DetallePedidoDAO detallePedidoDAO;
    private final ProductoDAO productoDAO;
    private final CategoriaDAO categoriaDAO;
    private final PagoDAO pagoDAO;
    private final PromocionDAO promocionDAO;
    private final ProveedorFacturaElectronica proveedorFE;
    private final PoliRestauranteProperties props;
    private final Map<MedioPago, MedioPagoStrategy> estrategias;

    public CajeroService(CajeroDAO cajeroDAO, CajaDAO cajaDAO, PedidoDAO pedidoDAO, PedidoMesaDAO pedidoMesaDAO,
                         MesaDAO mesaDAO, MeseroDAO meseroDAO, UsuarioDAO usuarioDAO,
                         DetallePedidoDAO detallePedidoDAO, ProductoDAO productoDAO, CategoriaDAO categoriaDAO,
                         PagoDAO pagoDAO, PromocionDAO promocionDAO, ProveedorFacturaElectronica proveedorFE,
                         PoliRestauranteProperties props, List<MedioPagoStrategy> estrategias) {
        this.cajeroDAO = cajeroDAO;
        this.cajaDAO = cajaDAO;
        this.pedidoDAO = pedidoDAO;
        this.pedidoMesaDAO = pedidoMesaDAO;
        this.mesaDAO = mesaDAO;
        this.meseroDAO = meseroDAO;
        this.usuarioDAO = usuarioDAO;
        this.detallePedidoDAO = detallePedidoDAO;
        this.productoDAO = productoDAO;
        this.categoriaDAO = categoriaDAO;
        this.pagoDAO = pagoDAO;
        this.promocionDAO = promocionDAO;
        this.proveedorFE = proveedorFE;
        this.props = props;
        this.estrategias = estrategias.stream()
                .collect(Collectors.toMap(MedioPagoStrategy::medio, Function.identity()));
    }

    // ==================================================================
    // Caja (RF11) - apertura, cierre y reporte de ventas del turno
    // ==================================================================

    @Transactional(readOnly = true)
    public Optional<ResumenCajaDTO> consultarCajaActual(UsuarioSesion sesion) {
        return buscarCajaAbierta(cajeroId(sesion)).map(this::resumenCaja);
    }

    @Transactional
    public ResumenCajaDTO abrirCaja(UsuarioSesion sesion, double montoInicial) {
        Integer cajeroId = cajeroId(sesion);
        if (buscarCajaAbierta(cajeroId).isPresent()) {
            throw new ReglaNegocioException("Ya tiene una caja abierta");
        }
        Caja c = new Caja();
        c.setCajeroId(cajeroId);
        c.setMontoInicial(CalculadoraCuenta.redondear(montoInicial));
        c.setFechaApertura(LocalDateTime.now());
        c.setEstado(EstadoCaja.ABIERTA);
        cajaDAO.save(c);
        return resumenCaja(c);
    }

    /** Cierre de caja: guarda el monto contado y devuelve el reporte de ventas del turno. */
    @Transactional
    public ResumenCajaDTO cerrarCaja(UsuarioSesion sesion, double montoFinal) {
        Caja c = cajaAbierta(cajeroId(sesion));
        ResumenCajaDTO previo = resumenCaja(c);
        c.setMontoFinal(CalculadoraCuenta.redondear(montoFinal));
        c.setTotalVentas(previo.totalVentas());
        c.setFechaCierre(LocalDateTime.now());
        c.setEstado(EstadoCaja.CERRADA);
        cajaDAO.save(c);
        return resumenCaja(c);
    }

    @Transactional(readOnly = true)
    public List<ResumenCajaDTO> historialCajas(UsuarioSesion sesion) {
        return cajaDAO.findTop10ByCajeroIdOrderByIdCajaDesc(cajeroId(sesion)).stream().map(this::resumenCaja).toList();
    }

    private ResumenCajaDTO resumenCaja(Caja c) {
        List<Pago> pagos = c.getIdCaja() == null ? List.of() : pagoDAO.findByCajaIdAndEstado(c.getIdCaja(), EstadoPago.APROBADO);
        double total = CalculadoraCuenta.redondear(pagos.stream().mapToDouble(Pago::getMonto).sum());

        Map<MedioPago, double[]> porMedio = new EnumMap<>(MedioPago.class);
        for (Pago p : pagos) {
            double[] acc = porMedio.computeIfAbsent(p.getMetodoPago(), k -> new double[2]);
            acc[0]++;
            acc[1] += p.getMonto();
        }
        List<SerieValor> medios = porMedio.entrySet().stream()
                .map(e -> new SerieValor(e.getKey().name(), CalculadoraCuenta.redondear(e.getValue()[1]), (long) e.getValue()[0]))
                .toList();
        double efectivo = porMedio.containsKey(MedioPago.EFECTIVO) ? porMedio.get(MedioPago.EFECTIVO)[1] : 0;
        double esperado = CalculadoraCuenta.redondear((c.getMontoInicial() == null ? 0 : c.getMontoInicial()) + efectivo);
        Double diferencia = c.getMontoFinal() == null ? null : CalculadoraCuenta.redondear(c.getMontoFinal() - esperado);

        // "C-producto": inventario de lo vendido en el turno
        List<ItemVendido> vendidos = List.of();
        if (!pagos.isEmpty()) {
            Map<Integer, Producto> productos = productoDAO.findAll().stream()
                    .collect(Collectors.toMap(Producto::getIdProducto, Function.identity()));
            Map<Integer, long[]> unidades = new HashMap<>();
            Map<Integer, Double> montos = new HashMap<>();
            detallePedidoDAO.findByPedidoIdIn(pagos.stream().map(Pago::getPedidoId).toList()).stream()
                    .filter(d -> d.getEstado() != EstadoDetalle.ANULADO)
                    .forEach(d -> {
                        unidades.computeIfAbsent(d.getProductoId(), k -> new long[1])[0] += d.getCantidad();
                        montos.merge(d.getProductoId(), precio(d, productos) * d.getCantidad(), Double::sum);
                    });
            vendidos = unidades.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
                    .map(e -> new ItemVendido(
                            productos.containsKey(e.getKey()) ? productos.get(e.getKey()).getNombre() : "#" + e.getKey(),
                            e.getValue()[0], CalculadoraCuenta.redondear(montos.get(e.getKey()))))
                    .toList();
        }
        return new ResumenCajaDTO(c.getIdCaja(), c.getEstado(), c.getFechaApertura(), c.getFechaCierre(),
                c.getMontoInicial(), c.getMontoFinal(), total, pagos.size(), medios, esperado, diferencia, vendidos);
    }

    private Optional<Caja> buscarCajaAbierta(Integer cajeroId) {
        return cajaDAO.findFirstByCajeroIdAndEstadoOrderByIdCajaDesc(cajeroId, EstadoCaja.ABIERTA);
    }

    /** Prerrequisito "Caja abierta" de CU-CAJ-001, CU-CAJ-002 y CU-CAJ-003. */
    private Caja cajaAbierta(Integer cajeroId) {
        return buscarCajaAbierta(cajeroId)
                .orElseThrow(() -> new ReglaNegocioException("Debe realizar la apertura de caja antes de esta operación"));
    }

    private Integer cajeroId(UsuarioSesion sesion) {
        return cajeroDAO.findByUsuarioId(sesion.id())
                .orElseThrow(() -> new ReglaNegocioException("El usuario no está registrado como cajero"))
                .getIdCajero();
    }

    // ==================================================================
    // Cuentas (RF12, RF13, RF14, RF15, RF17)
    // ==================================================================

    /** RF12/RF13: cuentas abiertas creadas por cualquier mesero o cajero. */
    @Transactional(readOnly = true)
    public List<CuentaResumenDTO> consultarCuentas() {
        List<Pedido> pedidos = pedidoDAO.findByEstadoNotInOrderByFechaCreacionAsc(EstadoPedido.CERRADOS);
        if (pedidos.isEmpty()) return List.of();
        List<Integer> ids = pedidos.stream().map(Pedido::getIdPedido).toList();
        Map<Integer, List<DetallePedido>> detalles = detallePedidoDAO.findByPedidoIdIn(ids).stream()
                .collect(Collectors.groupingBy(DetallePedido::getPedidoId));
        Map<Integer, Producto> productos = productosPorId();
        Map<Integer, Integer> mesas = mesasDePedidos(ids);
        Map<Integer, String> meseros = nombresMeseros();

        return pedidos.stream().map(p -> {
            List<DetallePedido> ds = detalles.getOrDefault(p.getIdPedido(), List.of());
            Promocion promo = promocionDAO.buscarPorCodigo(p.getPromocionAplicada()).orElse(null);
            CalculadoraCuenta.Totales t = CalculadoraCuenta.calcular(lineas(ds, productos), promo, impuestoPct());
            int activos = (int) ds.stream().filter(d -> d.getEstado() != EstadoDetalle.ANULADO).count();
            return new CuentaResumenDTO(p.getIdPedido(), mesas.get(p.getIdPedido()), p.getTipoPedido(), p.getEstado(),
                    meseros.get(p.getMeseroId()), p.getFechaCreacion(), activos, t.total(),
                    promo == null ? null : promo.getNombre());
        }).toList();
    }

    /** consultarDetalleCuenta(idCuenta) de CU-CAJ-003: productos y valor total. */
    @Transactional(readOnly = true)
    public CuentaDetalleDTO consultarDetalleCuenta(Integer pedidoId) {
        return detalleCuenta(pedido(pedidoId), false);
    }

    /** RF14: pre-cuenta impresa solo con los productos marcados como entregados (HU-CAJ-002 AC4). */
    @Transactional(readOnly = true)
    public CuentaDetalleDTO comprobanteEntregados(Integer pedidoId) {
        return detalleCuenta(pedido(pedidoId), true);
    }

    private CuentaDetalleDTO detalleCuenta(Pedido p, boolean soloEntregados) {
        Map<Integer, Producto> productos = productosPorId();
        List<DetallePedido> ds = detallePedidoDAO.findByPedidoIdOrderByIdDetallePedidoAsc(p.getIdPedido()).stream()
                .filter(d -> !soloEntregados || d.getEstado() == EstadoDetalle.ENTREGADO)
                .toList();
        Promocion promo = promocionDAO.buscarPorCodigo(p.getPromocionAplicada()).orElse(null);
        List<CalculadoraCuenta.Linea> lineas = lineas(ds, productos);
        CalculadoraCuenta.Totales t = CalculadoraCuenta.calcular(lineas, promo, impuestoPct());
        Integer mesa = mesasDePedidos(List.of(p.getIdPedido())).get(p.getIdPedido());
        return new CuentaDetalleDTO(p.getIdPedido(), mesa, p.getTipoPedido(), p.getEstado(),
                nombresMeseros().get(p.getMeseroId()), p.getFechaCreacion(), items(ds, productos),
                t.subtotal(), t.descuento(), promo == null ? null : toDTO(promo, lineas),
                t.impuesto(), props.getImpuesto().getNombre(), t.total());
    }

    /** CU-CAJ-003: anular un producto de la cuenta y guardar el registro para control interno. */
    @Transactional
    public CuentaDetalleDTO anularProducto(UsuarioSesion sesion, Integer pedidoId, Integer detalleId, String motivo) {
        cajaAbierta(cajeroId(sesion));
        Pedido p = pedidoAbiertoBloqueado(pedidoId);
        DetallePedido d = detallePedidoDAO.findById(detalleId)
                .filter(x -> x.getPedidoId().equals(pedidoId))
                .orElseThrow(() -> new RecursoNoEncontradoException("El producto no pertenece a esta cuenta"));
        if (d.getEstado() == EstadoDetalle.ANULADO) {
            throw new ReglaNegocioException("El producto ya está anulado");
        }
        d.setEstado(EstadoDetalle.ANULADO);
        d.setFechaAnulacion(LocalDateTime.now());
        d.setMotivoAnulacion(motivo.trim());
        d.setUsuarioAnulacion(sesion.id());
        detallePedidoDAO.save(d);
        p.setFechaUltimaActualizacion(LocalDateTime.now());
        pedidoDAO.save(p);
        log.info("Producto anulado: pedido={} detalle={} usuario={} motivo={}", pedidoId, detalleId, sesion.nombre(), motivo);
        return detalleCuenta(p, false);
    }

    /** RF17: anular (cancelar) una cuenta que aun no ha sido pagada. */
    @Transactional
    public void cancelarCuenta(UsuarioSesion sesion, Integer pedidoId, String motivo) {
        cajaAbierta(cajeroId(sesion));
        Pedido p = pedidoAbiertoBloqueado(pedidoId);
        p.setEstado(EstadoPedido.CANCELADO);
        p.setFechaUltimaActualizacion(LocalDateTime.now());
        pedidoDAO.save(p);
        log.info("Cuenta cancelada: pedido={} usuario={} motivo={}", pedidoId, sesion.nombre(), motivo);
    }

    // ==================================================================
    // Promociones (RF22, RF23 / CU-CAJ-002)
    // ==================================================================

    /** RF22: promociones vigentes; con pedidoId, solo las aplicables a esa cuenta. */
    @Transactional(readOnly = true)
    public List<PromocionDTO> consultarPromociones(Integer pedidoId) {
        List<Promocion> vigentes = promocionDAO.buscarPromocionesVigentes(LocalDateTime.now());
        if (pedidoId == null) {
            return vigentes.stream().map(pr -> toDTO(pr, null)).toList();
        }
        List<CalculadoraCuenta.Linea> lineas = lineas(
                detallePedidoDAO.findByPedidoIdOrderByIdDetallePedidoAsc(pedido(pedidoId).getIdPedido()), productosPorId());
        return vigentes.stream()
                .filter(pr -> CalculadoraCuenta.promocionAplica(lineas, pr))   // paso 7: filtra las aplicables
                .map(pr -> toDTO(pr, lineas))
                .toList();
    }

    /** aplicarPromocion(idCuenta, idPromocion): revalida y deja la promocion aplicada en la cuenta. */
    @Transactional
    public CuentaDetalleDTO aplicarPromocion(UsuarioSesion sesion, Integer pedidoId, String codigo) {
        cajaAbierta(cajeroId(sesion));
        Pedido p = pedidoAbiertoBloqueado(pedidoId);
        Promocion promo = promocionDAO.buscarPorCodigo(codigo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Promoción no encontrada"));
        if (!promo.vigente(LocalDateTime.now())) {
            throw new ReglaNegocioException("La promoción '" + promo.getNombre() + "' no está vigente");
        }
        List<CalculadoraCuenta.Linea> lineas = lineas(
                detallePedidoDAO.findByPedidoIdOrderByIdDetallePedidoAsc(pedidoId), productosPorId());
        if (!CalculadoraCuenta.promocionAplica(lineas, promo)) {
            throw new ReglaNegocioException("La promoción no aplica a los productos de esta cuenta");
        }
        p.setPromocionAplicada(promo.getCodigo());
        p.setFechaUltimaActualizacion(LocalDateTime.now());
        pedidoDAO.save(p);
        return detalleCuenta(p, false);
    }

    @Transactional
    public CuentaDetalleDTO quitarPromocion(UsuarioSesion sesion, Integer pedidoId) {
        cajaAbierta(cajeroId(sesion));
        Pedido p = pedidoAbiertoBloqueado(pedidoId);
        p.setPromocionAplicada(null);
        pedidoDAO.save(p);
        return detalleCuenta(p, false);
    }

    private PromocionDTO toDTO(Promocion pr, List<CalculadoraCuenta.Linea> lineas) {
        String categoria = pr.getCategoriaId() == null ? null
                : categoriaDAO.findById(pr.getCategoriaId()).map(Categoria::getNombre).orElse(null);
        Double estimado = lineas == null ? null : CalculadoraCuenta.calcular(lineas, pr, impuestoPct()).descuento();
        return new PromocionDTO(pr.getCodigo(), pr.getNombre(), pr.getDescripcion(), pr.getPorcentaje(),
                pr.getCategoriaId(), categoria, pr.fin(), estimado);
    }

    // ==================================================================
    // Pago (RF16 / CU-CAJ-001 / HU-CAJ-001)
    // ==================================================================

    /**
     * registrarPago(idCuenta, medioPago): valida caja abierta y cuenta pendiente, procesa
     * el medio de pago con su estrategia y, si es aprobado, registra el pago y cierra la cuenta.
     * Si es rechazado no se guarda nada y el cajero puede reintentar (AC4).
     */
    @Transactional
    public PagoResultadoDTO registrarPago(UsuarioSesion sesion, PagoRequest req) {
        Integer cajeroId = cajeroId(sesion);
        Caja caja = cajaAbierta(cajeroId);
        Pedido pedido = pedidoAbiertoBloqueado(req.pedidoId());

        Map<Integer, Producto> productos = productosPorId();
        List<DetallePedido> detalles = detallePedidoDAO.findByPedidoIdOrderByIdDetallePedidoAsc(pedido.getIdPedido());
        Promocion promo = null;
        if (pedido.getPromocionAplicada() != null) {
            promo = promocionDAO.buscarPorCodigo(pedido.getPromocionAplicada()).orElse(null);
            if (promo == null || !promo.vigente(LocalDateTime.now())) {
                throw new ReglaNegocioException("La promoción aplicada ya no está vigente; quítela antes de cobrar");
            }
        }
        CalculadoraCuenta.Totales t = CalculadoraCuenta.calcular(lineas(detalles, productos), promo, impuestoPct());
        if (t.total() <= 0) {
            throw new ReglaNegocioException("La cuenta no tiene productos por cobrar");
        }

        MedioPagoStrategy estrategia = Optional.ofNullable(estrategias.get(req.medioPago()))
                .orElseThrow(() -> new ReglaNegocioException("Medio de pago no soportado"));
        ResultadoPago resultado = estrategia.procesar(
                new SolicitudCobro(pedido.getIdPedido(), t.total(), req.montoRecibido(), req.referencia()));
        if (!resultado.aprobado()) {
            throw new PagoRechazadoException(resultado.mensaje());
        }

        LocalDateTime ahora = LocalDateTime.now();
        // Congela el precio cobrado (la reimpresion mostrara lo mismo aunque el precio cambie)
        for (DetallePedido d : detalles) {
            if (d.getEstado() == EstadoDetalle.ANULADO) continue;
            d.setPrecioUnitario(precio(d, productos));
            d.setEstado(EstadoDetalle.ENTREGADO);
        }
        detallePedidoDAO.saveAll(detalles);

        Pago pago = new Pago();
        pago.setPedidoId(pedido.getIdPedido());
        pago.setMonto(t.total());
        pago.setSubtotal(t.subtotal());
        pago.setDescuento(t.descuento());
        pago.setImpuesto(t.impuesto());
        pago.setPromocion(promo == null ? null : promo.getNombre());
        pago.setMetodoPago(req.medioPago());
        pago.setReferencia(resultado.referencia());
        pago.setMontoRecibido(resultado.montoRecibido());
        pago.setCambio(resultado.cambio());
        pago.setFechaPago(ahora);
        pago.setEstado(EstadoPago.APROBADO);
        pago.setCajeroId(cajeroId);
        pago.setCajaId(caja.getIdCaja());
        pagoDAO.save(pago);

        pedido.setEstado(EstadoPedido.PAGADO);            // "UPDATE Pedido a Pagado" del diagrama
        pedido.setFechaUltimaActualizacion(ahora);
        pedidoDAO.save(pedido);

        return new PagoResultadoDTO(pago.getIdPago(), pedido.getIdPedido(), resultado.mensaje(), req.medioPago(),
                t.total(), resultado.montoRecibido(), resultado.cambio(), resultado.referencia());
    }

    // ==================================================================
    // Facturas (RF18, RF19, RF20, RF21 / HU-CAJ-002)
    // ==================================================================

    /** generarFactura(idCuenta, tipoFactura): fisica o electronica, una sola vez por pago (AC5). */
    @Transactional
    public FacturaDTO generarFactura(UsuarioSesion sesion, Integer pagoId, FacturaRequest req) {
        cajeroId(sesion);
        Pago pago = pago(pagoId);
        if (pago.getNumeroFactura() != null) {
            throw new ConflictoException("La factura " + pago.getNumeroFactura() + " ya fue emitida; use Reimprimir");
        }
        String correo = req.correoCliente() == null ? null : req.correoCliente().trim();
        if (req.tipoFactura() == TipoFactura.ELECTRONICA && (correo == null || correo.isBlank())) {
            throw new ReglaNegocioException("Para la factura electrónica indique el correo del cliente");
        }
        pago.setNumeroFactura(String.format("F-%06d", pago.getIdPago()));
        pago.setTipoFactura(req.tipoFactura());
        pago.setFechaFactura(LocalDateTime.now());
        pago.setCorreoCliente(req.tipoFactura() == TipoFactura.ELECTRONICA ? correo : null);

        String advertencia = null;
        if (req.tipoFactura() == TipoFactura.ELECTRONICA) {
            advertencia = emitirElectronica(pago);
        }
        pagoDAO.save(pago);
        return factura(pago, advertencia);
    }

    /** RNF10: si el proveedor de factura electronica fallo, se reintenta sin bloquear la cuenta. */
    @Transactional
    public FacturaDTO reintentarFacturaElectronica(UsuarioSesion sesion, Integer pagoId) {
        cajeroId(sesion);
        Pago pago = pago(pagoId);
        if (pago.getTipoFactura() != TipoFactura.ELECTRONICA || pago.getCodigoFacturaElectronica() != null) {
            throw new ReglaNegocioException("Esta factura no tiene una emisión electrónica pendiente");
        }
        String advertencia = emitirElectronica(pago);
        pagoDAO.save(pago);
        return factura(pago, advertencia);
    }

    private String emitirElectronica(Pago pago) {
        try {
            pago.setCodigoFacturaElectronica(
                    proveedorFE.emitir(pago.getNumeroFactura(), pago.getMonto(), pago.getCorreoCliente()));
            return null;
        } catch (IntegracionException e) {
            log.warn("Factura electronica {} pendiente: {}", pago.getNumeroFactura(), e.getMessage());
            return "El proveedor de factura electrónica no respondió. La cuenta quedó pagada; "
                    + "reintente el envío desde el historial de facturas.";
        }
    }

    /** RF18: historial de facturas con su metodo de pago (de todos los cajeros). */
    @Transactional(readOnly = true)
    public List<FacturaResumenDTO> historialFacturas(LocalDateTime desde, LocalDateTime hasta, MedioPago medio) {
        LocalDateTime d = desde != null ? desde : LocalDateTime.now().minusDays(30);
        LocalDateTime h = hasta != null ? hasta : LocalDateTime.now().plusMinutes(1);
        if (!h.isAfter(d)) {
            throw new ReglaNegocioException("La fecha final debe ser posterior a la fecha inicial");
        }
        List<Pago> pagos = pagoDAO.findByFechaPagoBetweenOrderByFechaPagoDesc(d, h).stream()
                .filter(p -> medio == null || p.getMetodoPago() == medio)
                .toList();
        Map<Integer, Integer> mesas = mesasDePedidos(pagos.stream().map(Pago::getPedidoId).toList());
        Map<Integer, String> cajeros = nombresCajeros();
        return pagos.stream().map(p -> new FacturaResumenDTO(p.getIdPago(), p.getNumeroFactura(), p.getTipoFactura(),
                estadoFactura(p), p.getFechaPago(), p.getPedidoId(), mesas.get(p.getPedidoId()),
                cajeros.get(p.getCajeroId()), p.getMetodoPago(), p.getMonto())).toList();
    }

    /** RF21: reimpresion de una factura del historial. */
    @Transactional(readOnly = true)
    public FacturaDTO reimprimirFactura(Integer pagoId) {
        Pago pago = pago(pagoId);
        if (pago.getNumeroFactura() == null) {
            throw new ReglaNegocioException("El pago aún no tiene factura emitida; genérela primero");
        }
        return factura(pago, null);
    }

    private String estadoFactura(Pago p) {
        if (p.getNumeroFactura() == null) return "SIN_EMITIR";
        if (p.getTipoFactura() == TipoFactura.ELECTRONICA && p.getCodigoFacturaElectronica() == null) return "PENDIENTE_FE";
        return "EMITIDA";
    }

    private FacturaDTO factura(Pago pago, String advertencia) {
        Map<Integer, Producto> productos = productosPorId();
        List<DetallePedido> ds = detallePedidoDAO.findByPedidoIdOrderByIdDetallePedidoAsc(pago.getPedidoId()).stream()
                .filter(d -> d.getEstado() == EstadoDetalle.ENTREGADO)
                .toList();
        Integer mesa = mesasDePedidos(List.of(pago.getPedidoId())).get(pago.getPedidoId());
        return new FacturaDTO(pago.getIdPago(), pago.getNumeroFactura(), pago.getTipoFactura(), pago.getFechaFactura(),
                pago.getFechaPago(), pago.getPedidoId(), mesa, nombresCajeros().get(pago.getCajeroId()),
                pago.getMetodoPago(), pago.getReferencia(), pago.getMontoRecibido(), pago.getCambio(),
                items(ds, productos), pago.getSubtotal() == null ? pago.getMonto() : pago.getSubtotal(),
                pago.getDescuento(), pago.getPromocion(), pago.getImpuesto(), props.getImpuesto().getNombre(),
                pago.getMonto(), pago.getCodigoFacturaElectronica(), pago.getCorreoCliente(), advertencia);
    }

    // ==================================================================
    // Utilidades
    // ==================================================================

    private Pedido pedido(Integer id) {
        return pedidoDAO.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Cuenta no encontrada"));
    }

    /** "Valida cuenta pendiente": existe, no esta pagada ni cancelada, y queda bloqueada en la transaccion. */
    private Pedido pedidoAbiertoBloqueado(Integer id) {
        Pedido p = pedidoDAO.bloquearPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta no encontrada"));
        if (p.getEstado() != null && p.getEstado().cerrado()) {
            throw new ReglaNegocioException("La cuenta ya está " + p.getEstado().name().toLowerCase());
        }
        return p;
    }

    private Pago pago(Integer id) {
        return pagoDAO.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Factura no encontrada"));
    }

    private double impuestoPct() {
        return props.getImpuesto().getPorcentaje();
    }

    private double precio(DetallePedido d, Map<Integer, Producto> productos) {
        if (d.getPrecioUnitario() != null) return d.getPrecioUnitario();
        Producto p = productos.get(d.getProductoId());
        return p == null || p.getPrecio() == null ? 0 : p.getPrecio();
    }

    private List<CalculadoraCuenta.Linea> lineas(List<DetallePedido> ds, Map<Integer, Producto> productos) {
        return ds.stream().map(d -> {
            Producto p = productos.get(d.getProductoId());
            return new CalculadoraCuenta.Linea(p == null ? null : p.getCategoriaId(), precio(d, productos),
                    d.getCantidad() == null ? 0 : d.getCantidad(), d.getEstado() == EstadoDetalle.ANULADO);
        }).toList();
    }

    private List<ItemCuentaDTO> items(List<DetallePedido> ds, Map<Integer, Producto> productos) {
        return ds.stream().map(d -> {
            Producto p = productos.get(d.getProductoId());
            double precio = precio(d, productos);
            int cantidad = d.getCantidad() == null ? 0 : d.getCantidad();
            return new ItemCuentaDTO(d.getIdDetallePedido(), d.getProductoId(), p == null ? "#" + d.getProductoId() : p.getNombre(),
                    p == null ? null : p.getCategoriaId(), precio, cantidad, d.getNotas(),
                    CalculadoraCuenta.redondear(precio * cantidad), d.getEstado(), d.getMotivoAnulacion());
        }).toList();
    }

    private Map<Integer, Producto> productosPorId() {
        return productoDAO.findAll().stream().collect(Collectors.toMap(Producto::getIdProducto, Function.identity()));
    }

    private Map<Integer, Integer> mesasDePedidos(Collection<Integer> pedidoIds) {
        if (pedidoIds.isEmpty()) return Map.of();
        Map<Integer, Integer> numeroMesa = Mapas.indexar(mesaDAO.findAll(), Mesa::getIdMesa, Mesa::getNumero);
        Map<Integer, Integer> r = new HashMap<>();
        pedidoMesaDAO.findByPedidoIdIn(pedidoIds).forEach(pm -> r.put(pm.getPedidoId(), numeroMesa.get(pm.getMesaId())));
        return r;
    }

    private Map<Integer, String> nombresMeseros() {
        Map<Integer, String> usuarios = Mapas.indexar(usuarioDAO.findAll(), Usuario::getIdUsuario, Usuario::getNombre);
        Map<Integer, String> r = new HashMap<>();
        meseroDAO.findAll().forEach(m -> r.put(m.getIdMesero(), usuarios.get(m.getUsuarioId())));
        return r;
    }

    private Map<Integer, String> nombresCajeros() {
        Map<Integer, String> usuarios = Mapas.indexar(usuarioDAO.findAll(), Usuario::getIdUsuario, Usuario::getNombre);
        Map<Integer, String> r = new HashMap<>();
        cajeroDAO.findAll().forEach(c -> r.put(c.getIdCajero(), usuarios.get(c.getUsuarioId())));
        return r;
    }
}
