package com.polirestaurante.service;

import com.polirestaurante.dao.*;
import com.polirestaurante.dto.MeseroDTO;
import com.polirestaurante.entity.*;
import com.polirestaurante.enums.EstadoDetalle;
import com.polirestaurante.enums.EstadoMesa;
import com.polirestaurante.enums.EstadoPedido;
import com.polirestaurante.enums.EstadoUsuario;
import com.polirestaurante.enums.Rol;
import com.polirestaurante.exception.AutenticacionException;
import com.polirestaurante.exception.RecursoNoEncontradoException;
import com.polirestaurante.exception.ReglaNegocioException;
import com.polirestaurante.security.UsuarioSesion;
import com.polirestaurante.service.mesero.ReglasMesero;
import com.polirestaurante.service.observer.EventoCocina;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Módulo Mesero (RF01-RF06, HU-MES-001/002/003, CU-MES-01/02).
 * Capas: MeseroController → MeseroService → DAO existentes → DB_Polirestaurante (sin tablas nuevas).
 * Las reglas puras viven en {@link ReglasMesero}. Cada cambio publica un {@link EventoCocina}
 * (Observer) que se entrega por WebSocket después del commit.
 */
@Service
public class MeseroService {
    private static final Logger log = LoggerFactory.getLogger(MeseroService.class);
    private static final String TIPO_SALON = "MESA";

    private final MesaDAO mesas;
    private final ProductoDAO productos;
    private final CategoriaDAO categorias;
    private final PedidoDAO pedidos;
    private final DetallePedidoDAO detalles;
    private final PedidoMesaDAO pedidoMesas;
    private final MeseroDAO meseros;
    private final UsuarioDAO usuarios;
    private final ApplicationEventPublisher eventos;

    public MeseroService(MesaDAO mesas, ProductoDAO productos, CategoriaDAO categorias, PedidoDAO pedidos,
                         DetallePedidoDAO detalles, PedidoMesaDAO pedidoMesas, MeseroDAO meseros,
                         UsuarioDAO usuarios, ApplicationEventPublisher eventos) {
        this.mesas = mesas; this.productos = productos; this.categorias = categorias; this.pedidos = pedidos;
        this.detalles = detalles; this.pedidoMesas = pedidoMesas; this.meseros = meseros;
        this.usuarios = usuarios; this.eventos = eventos;
    }

    private static LocalDateTime ahora() { return LocalDateTime.now(ZoneId.of("America/Bogota")); }
    private static double dinero(double v) { return CalculadoraCuenta.redondear(v); }
    private static double precio(Producto p) { return p.getPrecio() == null ? 0 : p.getPrecio(); }

    /** Revalida al actor en cada llamada, incluso si cambió de rol o se desactivó tras emitir el JWT. */
    private Mesero actor(UsuarioSesion sesion) {
        if (sesion == null || sesion.id() == null || sesion.rol() != Rol.MESERO)
            throw new AutenticacionException("La sesión no corresponde a un mesero activo.");
        boolean activo = usuarios.findById(sesion.id()).map(u -> u.getEstado() == EstadoUsuario.activo).orElse(false);
        return meseros.findByUsuarioId(sesion.id()).filter(m -> activo)
            .orElseThrow(() -> new AutenticacionException("La sesión no corresponde a un mesero activo."));
    }

    // ------------------------------------------------------------------ consultas

    /** Mesas activas con su estado y, si tienen cuenta abierta, el pedido (RF06). */
    @Transactional(readOnly = true)
    public List<MeseroDTO.Mesa> consultarMesas(UsuarioSesion sesion) {
        Mesero yo = actor(sesion);
        Map<Integer, Integer> pedidoPorMesa = new HashMap<>();
        pedidoMesas.vinculosConPedidoActivo(EstadoPedido.CERRADOS)
            .forEach(v -> pedidoPorMesa.merge(v.getMesaId(), v.getPedidoId(), Math::max));
        Map<Integer, Pedido> cuentas = pedidos.findAllById(pedidoPorMesa.values()).stream()
            .collect(Collectors.toMap(Pedido::getIdPedido, Function.identity()));
        return mesas.findByActivaTrueOrderByNumeroAsc().stream().map(m -> {
            Integer pid = pedidoPorMesa.get(m.getIdMesa());
            Pedido p = pid == null ? null : cuentas.get(pid);
            return new MeseroDTO.Mesa(m.getIdMesa(), m.getNumero(), m.getCapacidad(), m.getEstado(), pid,
                p != null && Objects.equals(p.getMeseroId(), yo.getIdMesero()));
        }).toList();
    }

    /** Carta que se puede ofrecer: activa, disponible y con existencias (criterio de Cocina). */
    @Transactional(readOnly = true)
    public List<MeseroDTO.Producto> consultarMenu(UsuarioSesion sesion) {
        actor(sesion);
        Map<Integer, String> cats = categorias.findAll().stream()
            .collect(Collectors.toMap(Categoria::getIdCategoria, c -> c.getNombre() == null ? "" : c.getNombre()));
        return productos.findByActivoTrueOrderByNombreAsc().stream()
            .filter(p -> Boolean.TRUE.equals(p.getDisponible()) && p.getCantidadDisponible() != null && p.getCantidadDisponible() > 0)
            .map(p -> new MeseroDTO.Producto(p.getIdProducto(), p.getNombre(), p.getDescripcion(),
                cats.getOrDefault(p.getCategoriaId(), "Sin categoría"), precio(p), p.getCantidadDisponible()))
            .sorted(Comparator.comparing(MeseroDTO.Producto::categoria, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(MeseroDTO.Producto::nombre, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    /** RF05: pedidos del mesero con su estado actual; abiertos y los cerrados en las últimas 24 h. */
    @Transactional(readOnly = true)
    public List<MeseroDTO.Pedido> consultarMisPedidos(UsuarioSesion sesion) {
        Mesero yo = actor(sesion);
        LocalDateTime desde = ahora().minusHours(24);
        return convertir(pedidos.findByMeseroIdOrderByFechaCreacionDesc(yo.getIdMesero()).stream()
            .filter(p -> ReglasMesero.cuentaAbierta(p.getEstado())
                || (p.getFechaCreacion() != null && p.getFechaCreacion().isAfter(desde))).toList());
    }

    @Transactional(readOnly = true)
    public MeseroDTO.Pedido consultarPedido(Integer id, UsuarioSesion sesion) {
        Mesero yo = actor(sesion);
        Pedido p = pedidos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado."));
        verificarPropietario(p, yo);
        return convertir(List.of(p)).getFirst();
    }

    // ------------------------------------------------------------------ mesas (RF06)

    @Transactional
    public MeseroDTO.Mesa ocuparMesa(Integer mesaId, UsuarioSesion sesion) { return cambiarEstadoMesa(mesaId, sesion, EstadoMesa.OCUPADA); }

    @Transactional
    public MeseroDTO.Mesa liberarMesa(Integer mesaId, UsuarioSesion sesion) { return cambiarEstadoMesa(mesaId, sesion, EstadoMesa.LIBRE); }

    private MeseroDTO.Mesa cambiarEstadoMesa(Integer mesaId, UsuarioSesion sesion, EstadoMesa nuevo) {
        actor(sesion);
        Mesa mesa = mesaBloqueada(mesaId);
        if (!nuevo.name().equals(mesa.getEstado())) {
            ReglasMesero.validarCambioMesa(Boolean.TRUE.equals(mesa.getActiva()), cuentasAbiertas(mesaId));
            mesa.setEstado(nuevo.name());
            mesas.saveAndFlush(mesa);
        } else if (!Boolean.TRUE.equals(mesa.getActiva())) {
            throw new ReglaNegocioException("La mesa está desactivada.");
        }
        return new MeseroDTO.Mesa(mesa.getIdMesa(), mesa.getNumero(), mesa.getCapacidad(), mesa.getEstado(), null, false);
    }

    // ------------------------------------------------------------------ pedidos (RF01, RF02)

    /** RF01 / HU-MES-001: crea el pedido de salón, descuenta existencias y ocupa la mesa. */
    @Transactional
    public MeseroDTO.Pedido crearPedido(MeseroDTO.NuevoPedido req, UsuarioSesion sesion) {
        Mesero yo = actor(sesion);
        if (req == null || req.mesaId() == null) throw new ReglaNegocioException("Seleccione la mesa del pedido.");
        if (req.lineas() == null || req.lineas().isEmpty())
            throw new ReglaNegocioException("El pedido requiere al menos un producto.");
        req.lineas().forEach(l -> ReglasMesero.validarCantidad(l.cantidad()));

        Mesa mesa = mesaBloqueada(req.mesaId());
        ReglasMesero.validarMesaParaPedido(Boolean.TRUE.equals(mesa.getActiva()), cuentasAbiertas(mesa.getIdMesa()));

        LocalDateTime ahora = ahora();
        Pedido pedido = new Pedido();
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setTipoPedido(TIPO_SALON);
        pedido.setMeseroId(yo.getIdMesero());
        pedido.setFechaCreacion(ahora);
        pedido.setFechaUltimaActualizacion(ahora);
        pedido = pedidos.saveAndFlush(pedido);

        Map<Integer, Integer> reservas = new TreeMap<>();   // orden fijo: evita bloqueos cruzados entre pedidos
        for (MeseroDTO.LineaRequest l : req.lineas()) {
            Producto prod = productoDisponible(l.productoId());
            DetallePedido d = nuevoDetalle(pedido.getIdPedido(), prod, l.cantidad(), l.notas());
            detalles.save(d);
            reservas.merge(prod.getIdProducto(), l.cantidad(), Integer::sum);
        }
        reservar(reservas);

        PedidoMesa v = new PedidoMesa();
        v.setPedidoId(pedido.getIdPedido());
        v.setMesaId(mesa.getIdMesa());
        pedidoMesas.save(v);
        if (!EstadoMesa.OCUPADA.name().equals(mesa.getEstado())) {
            mesa.setEstado(EstadoMesa.OCUPADA.name());
            mesas.save(mesa);
        }
        detalles.flush();
        return publicar(pedido.getIdPedido(), yo);
    }

    /** RF02: agrega un producto a un pedido PENDIENTE. */
    @Transactional
    public MeseroDTO.Pedido agregarProducto(Integer pedidoId, MeseroDTO.LineaRequest l, UsuarioSesion sesion) {
        Mesero yo = actor(sesion);
        if (l == null) throw new ReglaNegocioException("Indique el producto y la cantidad.");
        Pedido pedido = pedidoEditable(pedidoId, yo);
        ReglasMesero.validarCantidad(l.cantidad());
        Producto prod = productoDisponible(l.productoId());
        reservar(Map.of(prod.getIdProducto(), l.cantidad()));
        detalles.saveAndFlush(nuevoDetalle(pedido.getIdPedido(), prod, l.cantidad(), l.notas()));
        tocar(pedido);
        return publicar(pedido.getIdPedido(), yo);
    }

    /** RF02: la línea se anula (no se borra), las existencias vuelven y queda quién y cuándo. */
    @Transactional
    public MeseroDTO.Pedido eliminarProducto(Integer pedidoId, Integer detalleId, UsuarioSesion sesion) {
        Mesero yo = actor(sesion);
        Pedido pedido = pedidoEditable(pedidoId, yo);
        List<DetallePedido> lineas = detalles.findByPedidoIdOrderByIdDetallePedidoAsc(pedidoId);
        DetallePedido d = lineas.stream().filter(x -> x.getIdDetallePedido().equals(detalleId)
                && x.getEstado() != EstadoDetalle.ANULADO).findFirst()
            .orElseThrow(() -> new ReglaNegocioException("El producto no está en el pedido."));
        if (lineas.stream().filter(x -> x.getEstado() != EstadoDetalle.ANULADO).count() <= 1)
            throw new ReglaNegocioException("El pedido debe conservar al menos un producto. Puede cancelarlo.");
        d.setEstado(EstadoDetalle.ANULADO);
        d.setFechaAnulacion(ahora());
        d.setMotivoAnulacion("Retirado por el mesero antes de preparación");
        d.setUsuarioAnulacion(sesion.id());
        detalles.saveAndFlush(d);
        productos.devolver(d.getProductoId(), d.getCantidad());
        tocar(pedido);
        return publicar(pedidoId, yo);
    }

    // ------------------------------------------------------------------ estados (RF03, RF04)

    /** RF03: entrega al cliente un pedido que cocina dejó LISTO. */
    @Transactional
    public MeseroDTO.Pedido entregarPedido(Integer pedidoId, UsuarioSesion sesion) {
        Mesero yo = actor(sesion);
        Pedido pedido = pedidoDelMesero(pedidoId, yo);
        ReglasMesero.validarEntrega(pedido.getEstado());
        LocalDateTime ahora = ahora();
        pedido.setEstado(EstadoPedido.ENTREGADO);
        pedido.setFechaEntregado(ahora);
        pedido.setFechaUltimaActualizacion(ahora);
        pedidos.saveAndFlush(pedido);
        detalles.findByPedidoIdOrderByIdDetallePedidoAsc(pedidoId).stream()
            .filter(d -> d.getEstado() == EstadoDetalle.PENDIENTE)
            .forEach(d -> d.setEstado(EstadoDetalle.ENTREGADO));
        detalles.flush();
        return publicar(pedidoId, yo);
    }

    /** RF04 / HU-MES-002: cancela PENDIENTE o EN_PREPARACION; libera la mesa si era su única cuenta. */
    @Transactional
    public MeseroDTO.Pedido cancelarPedido(Integer pedidoId, String motivo, UsuarioSesion sesion) {
        Mesero yo = actor(sesion);
        Pedido pedido = pedidoDelMesero(pedidoId, yo);
        ReglasMesero.validarCancelacion(pedido.getEstado());
        EstadoPedido anterior = pedido.getEstado();
        pedido.setEstado(EstadoPedido.CANCELADO);
        pedido.setFechaUltimaActualizacion(ahora());
        pedidos.saveAndFlush(pedido);
        if (ReglasMesero.devuelveExistencias(anterior)) {
            Map<Integer, Integer> devoluciones = new TreeMap<>();
            detalles.findByPedidoIdOrderByIdDetallePedidoAsc(pedidoId).stream()
                .filter(d -> d.getEstado() != EstadoDetalle.ANULADO)
                .forEach(d -> devoluciones.merge(d.getProductoId(), d.getCantidad(), Integer::sum));
            devoluciones.forEach(productos::devolver);
        }
        // El modelo compartido no tiene dónde guardar el motivo de una cancelación (ver docs/MESERO.md).
        log.info("Pedido {} cancelado por usuario {} (estado previo {}). Motivo: {}", pedidoId, sesion.id(), anterior,
            motivo == null || motivo.isBlank() ? "no indicado" : motivo.trim());
        liberarMesaSiCorresponde(pedidoId);
        return publicar(pedidoId, yo);
    }

    // ------------------------------------------------------------------ utilidades

    private Pedido pedidoDelMesero(Integer pedidoId, Mesero yo) {
        Pedido p = pedidos.bloquearPorId(pedidoId).orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado."));
        verificarPropietario(p, yo);
        return p;
    }

    private Pedido pedidoEditable(Integer pedidoId, Mesero yo) {
        Pedido p = pedidoDelMesero(pedidoId, yo);
        ReglasMesero.exigirEditable(p.getEstado());
        return p;
    }

    private static void verificarPropietario(Pedido p, Mesero yo) {
        if (!Objects.equals(p.getMeseroId(), yo.getIdMesero()))
            throw new ReglaNegocioException("Este pedido pertenece a otro mesero.");
    }

    private Mesa mesaBloqueada(Integer id) {
        if (id == null) throw new ReglaNegocioException("Mesa no encontrada.");
        return mesas.bloquearPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("Mesa no encontrada."));
    }

    private long cuentasAbiertas(Integer mesaId) {
        return pedidoMesas.contarPedidosActivos(mesaId, EstadoPedido.CERRADOS);
    }

    private Producto productoDisponible(Integer id) {
        if (id == null) throw new ReglaNegocioException("Producto no encontrado.");
        Producto p = productos.findById(id).orElseThrow(() -> new ReglaNegocioException("Producto no encontrado."));
        ReglasMesero.validarProductoDisponible(p.getNombre(), Boolean.TRUE.equals(p.getActivo()),
            Boolean.TRUE.equals(p.getDisponible()), p.getCantidadDisponible());
        return p;
    }

    /** Descuento atómico en orden fijo; si no alcanza, la excepción revierte toda la transacción. */
    private void reservar(Map<Integer, Integer> reservas) {
        reservas.forEach((id, cantidad) -> {
            if (productos.reservar(id, cantidad) != 1) {
                Producto p = productos.findById(id).orElseThrow(() -> new ReglaNegocioException("Producto no encontrado."));
                throw new ReglaNegocioException("No hay existencias suficientes de " + p.getNombre()
                    + ". Disponibles: " + (p.getCantidadDisponible() == null ? 0 : p.getCantidadDisponible()) + ".");
            }
        });
    }

    private static DetallePedido nuevoDetalle(Integer pedidoId, Producto prod, int cantidad, String notas) {
        DetallePedido d = new DetallePedido();
        d.setPedidoId(pedidoId);
        d.setProductoId(prod.getIdProducto());
        d.setCantidad(cantidad);
        d.setPrecioUnitario(precio(prod));      // el precio queda congelado (RF21)
        d.setEstado(EstadoDetalle.PENDIENTE);
        d.setNotas(notas == null || notas.isBlank() ? null : notas.trim());
        return d;
    }

    private void tocar(Pedido pedido) {
        pedido.setFechaUltimaActualizacion(ahora());
        pedidos.saveAndFlush(pedido);
    }

    private void liberarMesaSiCorresponde(Integer pedidoId) {
        pedidoMesas.findByPedidoIdIn(List.of(pedidoId)).forEach(v -> {
            Mesa mesa = mesaBloqueada(v.getMesaId());
            if (EstadoMesa.OCUPADA.name().equals(mesa.getEstado()) && cuentasAbiertas(mesa.getIdMesa()) == 0) {
                mesa.setEstado(EstadoMesa.LIBRE.name());
                mesas.save(mesa);
            }
        });
    }

    /** Observer: el evento se entrega al terminar la transacción, no antes. */
    private MeseroDTO.Pedido publicar(Integer pedidoId, Mesero yo) {
        Pedido p = pedidos.findById(pedidoId).orElseThrow();
        eventos.publishEvent(EventoCocina.pedido(pedidoId, yo.getIdMesero(), p.getEstado()));
        return convertir(List.of(p)).getFirst();
    }

    private List<MeseroDTO.Pedido> convertir(List<Pedido> lista) {
        if (lista.isEmpty()) return List.of();
        List<Integer> ids = lista.stream().map(Pedido::getIdPedido).toList();
        List<DetallePedido> lineas = detalles.findByPedidoIdIn(ids).stream()
            .sorted(Comparator.comparing(DetallePedido::getIdDetallePedido)).toList();
        Map<Integer, Producto> carta = productos.findAllById(lineas.stream().map(DetallePedido::getProductoId).distinct().toList())
            .stream().collect(Collectors.toMap(Producto::getIdProducto, Function.identity()));
        Map<Integer, List<DetallePedido>> porPedido = lineas.stream().collect(Collectors.groupingBy(DetallePedido::getPedidoId));
        List<PedidoMesa> vinculos = pedidoMesas.findByPedidoIdIn(ids);
        Map<Integer, Mesa> mapaMesas = mesas.findAllById(vinculos.stream().map(PedidoMesa::getMesaId).distinct().toList())
            .stream().collect(Collectors.toMap(Mesa::getIdMesa, Function.identity()));
        Map<Integer, PedidoMesa> mesaPorPedido = new HashMap<>();
        vinculos.forEach(v -> mesaPorPedido.putIfAbsent(v.getPedidoId(), v));
        return lista.stream().map(p -> {
            List<DetallePedido> ds = porPedido.getOrDefault(p.getIdPedido(), List.of());
            List<MeseroDTO.Linea> out = ds.stream().map(d -> {
                Producto prod = carta.get(d.getProductoId());
                double unit = d.getPrecioUnitario() != null ? d.getPrecioUnitario() : prod == null ? 0 : precio(prod);
                int cant = d.getCantidad() == null ? 0 : d.getCantidad();
                return new MeseroDTO.Linea(d.getIdDetallePedido(), d.getProductoId(),
                    prod == null ? "Producto #" + d.getProductoId() : prod.getNombre(), cant, unit, dinero(unit * cant),
                    d.getNotas(), d.getEstado() == EstadoDetalle.ANULADO, d.getMotivoAnulacion());
            }).toList();
            double total = CalculadoraCuenta.calcular(ds.stream().map(d -> {
                Producto prod = carta.get(d.getProductoId());
                double unit = d.getPrecioUnitario() != null ? d.getPrecioUnitario() : prod == null ? 0 : precio(prod);
                return new CalculadoraCuenta.Linea(prod == null ? null : prod.getCategoriaId(), unit,
                    d.getCantidad() == null ? 0 : d.getCantidad(), d.getEstado() == EstadoDetalle.ANULADO);
            }).toList(), null, 0).total();
            PedidoMesa v = mesaPorPedido.get(p.getIdPedido());
            Mesa m = v == null ? null : mapaMesas.get(v.getMesaId());
            EstadoPedido e = p.getEstado();
            return new MeseroDTO.Pedido(p.getIdPedido(), e, v == null ? null : v.getMesaId(), m == null ? null : m.getNumero(),
                p.getFechaCreacion(), p.getFechaUltimaActualizacion(), total,
                e == EstadoPedido.PENDIENTE, e == EstadoPedido.LISTO,
                e == EstadoPedido.PENDIENTE || e == EstadoPedido.EN_PREPARACION, out);
        }).toList();
    }
}
