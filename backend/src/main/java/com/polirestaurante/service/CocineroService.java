package com.polirestaurante.service;

import com.polirestaurante.dao.*;
import com.polirestaurante.dto.CocinaDTO;
import com.polirestaurante.entity.*;
import com.polirestaurante.enums.EstadoDetalle;
import com.polirestaurante.enums.EstadoPedido;
import com.polirestaurante.exception.RecursoNoEncontradoException;
import com.polirestaurante.exception.ReglaNegocioException;
import com.polirestaurante.security.AccesoCocina;
import com.polirestaurante.security.UsuarioSesion;
import com.polirestaurante.service.observer.EventoCocina;
import com.polirestaurante.service.state.DisponibilidadCocina;
import com.polirestaurante.service.state.EstadoCocina;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** RF07-RF10 y RF39: componente CocineroService del diagrama de arquitectura. */
@Service
public class CocineroService {
    private static final List<EstadoPedido> EXCLUIDOS = List.of(EstadoPedido.ENTREGADO, EstadoPedido.CANCELADO, EstadoPedido.PAGADO);
    private final PedidoDAO pedidos;
    private final DetallePedidoDAO detalles;
    private final ProductoDAO productos;
    private final CategoriaDAO categorias;
    private final PedidoMesaDAO pedidoMesas;
    private final MesaDAO mesas;
    private final MeseroDAO meseros;
    private final UsuarioDAO usuarios;
    private final CocinaAuditoriaDAO auditoria;
    private final AccesoCocina acceso;
    private final ApplicationEventPublisher eventos;

    public CocineroService(PedidoDAO pedidos, DetallePedidoDAO detalles, ProductoDAO productos,
                          CategoriaDAO categorias, PedidoMesaDAO pedidoMesas, MesaDAO mesas, MeseroDAO meseros,
                          UsuarioDAO usuarios, CocinaAuditoriaDAO auditoria, AccesoCocina acceso, ApplicationEventPublisher eventos) {
        this.pedidos = pedidos; this.detalles = detalles; this.productos = productos; this.categorias = categorias;
        this.pedidoMesas = pedidoMesas; this.mesas = mesas; this.meseros = meseros; this.usuarios = usuarios;
        this.auditoria = auditoria; this.acceso = acceso; this.eventos = eventos;
    }
    private static LocalDateTime ahora() { return LocalDateTime.now(ZoneId.of("America/Bogota")); }
    private static <T> Map<Integer,T> mapa(List<T> lista, Function<T,Integer> id) {
        return lista.stream().collect(Collectors.toMap(id, Function.identity()));
    }

    @Transactional(readOnly = true)
    public CocinaDTO.Tablero tablero(UsuarioSesion sesion) {
        acceso.exigirCocinero(sesion);
        return new CocinaDTO.Tablero(consultarPedidos(), listarDisponibilidad());
    }

    /** Para el observador WebSocket interno, que valida las sesiones antes de enviar. */
    @Transactional(readOnly = true)
    public CocinaDTO.Tablero instantanea() {
        return new CocinaDTO.Tablero(consultarPedidos(), listarDisponibilidad());
    }

    @Transactional(readOnly = true)
    public CocinaDTO.Orden consultarDetallePedido(Integer id, UsuarioSesion sesion) {
        acceso.exigirCocinero(sesion);
        Pedido p = pedidos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("El pedido no existe."));
        return convertirPedidos(List.of(p)).getFirst();
    }

    private List<CocinaDTO.Orden> consultarPedidos() {
        return convertirPedidos(pedidos.findByEstadoNotInOrderByFechaCreacionAsc(EXCLUIDOS).stream()
            .sorted(Comparator.comparing(Pedido::getFechaCreacion, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Pedido::getIdPedido)).toList());
    }

    private List<CocinaDTO.Orden> convertirPedidos(List<Pedido> lista) {
        if (lista.isEmpty()) return List.of();
        List<Integer> ids = lista.stream().map(Pedido::getIdPedido).toList();
        List<DetallePedido> lineas = detalles.findByPedidoIdIn(ids).stream()
            .filter(d -> d.getEstado() != EstadoDetalle.ANULADO)
            .sorted(Comparator.comparing(DetallePedido::getIdDetallePedido)).toList();
        Map<Integer, Producto> carta = mapa(productos.findAllById(lineas.stream().map(DetallePedido::getProductoId).distinct().toList()), Producto::getIdProducto);
        Map<Integer,List<DetallePedido>> porPedido = lineas.stream().collect(Collectors.groupingBy(DetallePedido::getPedidoId));
        List<PedidoMesa> vinculos = pedidoMesas.findByPedidoIdIn(ids);
        Map<Integer,Mesa> mapaMesas = mapa(mesas.findAllById(vinculos.stream().map(PedidoMesa::getMesaId).distinct().toList()), Mesa::getIdMesa);
        Map<Integer,List<PedidoMesa>> mesasPorPedido = vinculos.stream().collect(Collectors.groupingBy(PedidoMesa::getPedidoId));
        Map<Integer,Mesero> mapaMeseros = mapa(meseros.findAllById(lista.stream().map(Pedido::getMeseroId).filter(Objects::nonNull).distinct().toList()), Mesero::getIdMesero);
        Map<Integer,Usuario> mapaUsuarios = mapa(usuarios.findAllById(mapaMeseros.values().stream().map(Mesero::getUsuarioId).distinct().toList()), Usuario::getIdUsuario);
        var auditorias = auditoria.pedidos(ids);
        return lista.stream().map(p -> {
            var a = auditorias.getOrDefault(p.getIdPedido(), new CocinaAuditoriaDAO.AuditoriaPedido(null, null));
            var mesero = mapaMeseros.get(p.getMeseroId());
            var u = mesero == null ? null : mapaUsuarios.get(mesero.getUsuarioId());
            List<Integer> numeros = mesasPorPedido.getOrDefault(p.getIdPedido(), List.of()).stream()
                .map(v -> mapaMesas.get(v.getMesaId())).filter(Objects::nonNull).map(Mesa::getNumero)
                .filter(Objects::nonNull).distinct().sorted().toList();
            List<CocinaDTO.Item> items = porPedido.getOrDefault(p.getIdPedido(), List.of()).stream().map(d -> {
                var producto = carta.get(d.getProductoId());
                return new CocinaDTO.Item(d.getIdDetallePedido(), d.getProductoId(),
                    producto == null ? "Producto #" + d.getProductoId() : producto.getNombre(), d.getCantidad(), d.getNotas());
            }).toList();
            return new CocinaDTO.Orden(p.getIdPedido(), p.getEstado(), p.getFechaCreacion(), p.getFechaUltimaActualizacion(),
                p.getTipoPedido(), u == null ? "Mesero #" + p.getMeseroId() : u.getNombre(), numeros,
                p.getFechaEnPreparacion(), p.getFechaListo(), a.usuarioPreparacion(), a.usuarioListo(), items);
        }).toList();
    }

    private List<CocinaDTO.Producto> listarDisponibilidad() {
        var mapaCategorias = mapa(categorias.findAll(), Categoria::getIdCategoria);
        var cartaActiva = productos.findByActivoTrueOrderByNombreAsc();
        var auditorias = auditoria.productos(cartaActiva.stream().map(Producto::getIdProducto).toList());
        return cartaActiva.stream().map(p -> {
            var c = mapaCategorias.get(p.getCategoriaId());
            var a = auditorias.getOrDefault(p.getIdProducto(), new CocinaAuditoriaDAO.AuditoriaProducto(null, null));
            boolean disponible = Boolean.TRUE.equals(p.getDisponible());
            return new CocinaDTO.Producto(p.getIdProducto(), p.getNombre(), c == null ? "Sin categoría" : c.getNombre(),
                p.getCantidadDisponible(), disponible, DisponibilidadCocina.efectiva(disponible, p.getCantidadDisponible()), a.fecha(), a.usuario());
        }).toList();
    }

    @Transactional
    public void actualizarEstadoPedido(Integer idPedido, EstadoPedido nuevoEstado, UsuarioSesion sesion) {
        acceso.exigirCocinero(sesion);
        Pedido p = pedidos.bloquearPorId(idPedido).orElseThrow(() -> new RecursoNoEncontradoException("El pedido no existe."));
        EstadoCocina.validarTransicion(p.getEstado(), nuevoEstado);
        LocalDateTime fecha = ahora();
        p.setEstado(nuevoEstado);
        p.setFechaUltimaActualizacion(fecha);
        if (nuevoEstado == EstadoPedido.EN_PREPARACION) {
            p.setFechaEnPreparacion(fecha);
            auditoria.registrarInicio(idPedido, sesion.id());
        } else {
            p.setFechaListo(fecha);
            auditoria.registrarListo(idPedido, sesion.id());
        }
        pedidos.saveAndFlush(p);
        // El observador escucha AFTER_COMMIT; un rollback nunca genera un aviso de éxito.
        eventos.publishEvent(EventoCocina.pedido(idPedido, p.getMeseroId(), nuevoEstado));
    }

    @Transactional
    public void actualizarDisponibilidad(Integer idProducto, Boolean disponible, UsuarioSesion sesion) {
        acceso.exigirCocinero(sesion);
        if (disponible == null) throw new ReglaNegocioException("Indique si el producto está disponible.");
        Producto p = auditoria.bloquearProducto(idProducto).filter(x -> Boolean.TRUE.equals(x.getActivo()))
            .orElseThrow(() -> new RecursoNoEncontradoException("El producto no existe o ya fue eliminado del catálogo."));
        DisponibilidadCocina.validar(Boolean.TRUE.equals(p.getDisponible()), disponible, p.getCantidadDisponible());
        p.setDisponible(disponible); // no modifica cantidades ni los pedidos registrados
        productos.saveAndFlush(p);
        auditoria.registrarDisponibilidad(idProducto, sesion.id(), ahora());
        eventos.publishEvent(EventoCocina.disponibilidad(idProducto, disponible));
    }
}
