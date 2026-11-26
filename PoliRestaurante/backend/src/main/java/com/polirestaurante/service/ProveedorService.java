package com.polirestaurante.service;

import com.polirestaurante.dao.*;
import com.polirestaurante.dto.*;
import com.polirestaurante.entity.*;
import com.polirestaurante.enums.EstadoPagoProveedor;
import com.polirestaurante.enums.EstadoSolicitud;
import com.polirestaurante.enums.EstadoUsuario;
import com.polirestaurante.exception.*;
import com.polirestaurante.security.UsuarioSesion;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Componente ProveedorService del diagrama de componentes.
 * Administrador: registrar proveedores y sus insumos (RF32 / CU-ADM-09), solicitudes de
 * insumos (RF35), recepcion de mercancia (RF33) y pagos a proveedores (RF36).
 * Proveedor: historial de entregas (RF34), solicitudes recibidas (RF35), pagos (RF36).
 */
@Service
@RequiredArgsConstructor
public class ProveedorService {

    private static final List<EstadoSolicitud> ABIERTAS = List.of(EstadoSolicitud.PENDIENTE, EstadoSolicitud.DESPACHADA);

    private final ProveedorDAO proveedorDAO;
    private final UsuarioDAO usuarioDAO;
    private final AdministradorDAO administradorDAO;
    private final InsumoDAO insumoDAO;
    private final InsumoProveedorDAO insumoProveedorDAO;
    private final PedidoInsumoDAO pedidoInsumoDAO;
    private final DetallePedidoInsumoDAO detallePedidoInsumoDAO;
    private final PagoProveedorDAO pagoProveedorDAO;
    private final PasswordEncoder passwordEncoder;

    // ------------------------------------------------------------------
    // Proveedores (CU-ADM-09)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ProveedorDTO> listarProveedores() {
        return proveedorDAO.findAllByOrderByNombreAsc().stream().map(this::toDTO).toList();
    }

    /** registrarProveedor(datosProveedor) del diagrama de secuencia. */
    @Transactional
    public ProveedorDTO registrarProveedor(ProveedorRequest req) {
        String nombre = req.nombre().trim();
        String usuario = req.usuario().trim();
        if (proveedorDAO.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictoException("Ya existe un proveedor llamado '" + nombre + "'");
        }
        if (usuarioDAO.existsByNombre(usuario)) {
            throw new ConflictoException("El usuario '" + usuario + "' ya existe");
        }
        Usuario u = new Usuario();
        u.setNombre(usuario);
        u.setCredenciales(passwordEncoder.encode(req.password()));
        u.setEstado(EstadoUsuario.activo);
        usuarioDAO.save(u);

        Proveedor p = new Proveedor();
        p.setNombre(nombre);
        p.setContacto(req.contacto().trim());
        p.setUsuarioId(u.getIdUsuario());
        proveedorDAO.save(p);

        if (req.insumos() != null) {
            req.insumos().forEach(ip -> vincularInsumo(p.getIdProveedor(), ip));
        }
        return toDTO(p);
    }

    @Transactional
    public ProveedorDTO editarProveedor(Integer id, EditarProveedorRequest req) {
        Proveedor p = proveedor(id);
        if (proveedorDAO.existsByNombreIgnoreCaseAndIdProveedorNot(req.nombre().trim(), id)) {
            throw new ConflictoException("Ya existe un proveedor llamado '" + req.nombre().trim() + "'");
        }
        p.setNombre(req.nombre().trim());
        p.setContacto(req.contacto().trim());
        proveedorDAO.save(p);
        return toDTO(p);
    }

    /** Registra (o actualiza el precio de) un insumo que suministra el proveedor. */
    @Transactional
    public ProveedorDTO vincularInsumo(Integer proveedorId, InsumoPrecio req) {
        Proveedor p = proveedor(proveedorId);
        if (!insumoDAO.existsById(req.insumoId())) {
            throw new RecursoNoEncontradoException("Insumo no encontrado");
        }
        InsumoProveedor ip = insumoProveedorDAO.findByProveedorIdAndInsumoId(proveedorId, req.insumoId())
                .orElseGet(InsumoProveedor::new);
        ip.setProveedorId(proveedorId);
        ip.setInsumoId(req.insumoId());
        ip.setPrecio(CalculadoraCuenta.redondear(req.precio()));
        if (ip.getDisponible() == null) ip.setDisponible(true);
        insumoProveedorDAO.save(ip);
        return toDTO(p);
    }

    @Transactional
    public ProveedorDTO desvincularInsumo(Integer proveedorId, Integer relacionId) {
        InsumoProveedor ip = insumoProveedorDAO.findById(relacionId)
                .filter(x -> x.getProveedorId().equals(proveedorId))
                .orElseThrow(() -> new RecursoNoEncontradoException("El proveedor no suministra ese insumo"));
        insumoProveedorDAO.delete(ip);
        return toDTO(proveedor(proveedorId));
    }

    private ProveedorDTO toDTO(Proveedor p) {
        Map<Integer, Insumo> insumos = insumosPorId();
        List<InsumoSuministradoDTO> suministra = insumoProveedorDAO.findByProveedorId(p.getIdProveedor()).stream()
                .map(ip -> toDTO(ip, insumos))
                .sorted(Comparator.comparing(InsumoSuministradoDTO::insumo, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        Usuario u = p.getUsuarioId() == null ? null : usuarioDAO.findById(p.getUsuarioId()).orElse(null);
        double saldo = pagosDeProveedor(p.getIdProveedor()).stream()
                .filter(x -> x.estado() == EstadoPagoProveedor.PENDIENTE)
                .mapToDouble(PagoProveedorDTO::monto).sum();
        return new ProveedorDTO(p.getIdProveedor(), p.getNombre(), p.getContacto(),
                u == null ? null : u.getNombre(), u != null && u.getEstado() == EstadoUsuario.activo,
                suministra, CalculadoraCuenta.redondear(saldo));
    }

    private InsumoSuministradoDTO toDTO(InsumoProveedor ip, Map<Integer, Insumo> insumos) {
        Insumo i = insumos.get(ip.getInsumoId());
        return new InsumoSuministradoDTO(ip.getIdInsumoProveedor(), ip.getInsumoId(),
                i == null ? null : i.getNombre(), i == null ? null : i.getUnidadMedida(),
                ip.getPrecio(), Boolean.TRUE.equals(ip.getDisponible()));
    }

    // ------------------------------------------------------------------
    // Solicitudes de insumos (RF35) y recepcion de mercancia (RF33)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<SolicitudDTO> listarSolicitudes() {
        return toSolicitudes(pedidoInsumoDAO.findAllByOrderByFechaDesc());
    }

    @Transactional
    public SolicitudDTO crearSolicitud(UsuarioSesion sesion, SolicitudRequest req) {
        Integer adminId = administradorId(sesion);
        proveedor(req.proveedorId());
        Set<Integer> vistos = new HashSet<>();
        List<DetallePedidoInsumo> detalles = new ArrayList<>();
        for (ItemSolicitud item : req.items()) {
            if (!vistos.add(item.insumoId())) {
                throw new ReglaNegocioException("Un insumo aparece repetido en la solicitud");
            }
            InsumoProveedor ip = insumoProveedorDAO.findByProveedorIdAndInsumoId(req.proveedorId(), item.insumoId())
                    .orElseThrow(() -> new ReglaNegocioException("El proveedor no suministra uno de los insumos seleccionados"));
            if (!Boolean.TRUE.equals(ip.getDisponible())) {
                throw new ReglaNegocioException("El proveedor marcó como no disponible uno de los insumos seleccionados");
            }
            DetallePedidoInsumo d = new DetallePedidoInsumo();
            d.setInsumoId(item.insumoId());
            d.setCantidad(item.cantidad());
            d.setPrecioUnitario(ip.getPrecio());
            detalles.add(d);
        }
        PedidoInsumo s = new PedidoInsumo();
        s.setAdministradorId(adminId);
        s.setProveedorId(req.proveedorId());
        s.setFecha(LocalDateTime.now());
        s.setEstado(EstadoSolicitud.PENDIENTE);
        pedidoInsumoDAO.save(s);
        detalles.forEach(d -> d.setPedidoInsumoId(s.getIdPedidoInsumo()));
        detallePedidoInsumoDAO.saveAll(detalles);
        return toSolicitudes(List.of(s)).get(0);
    }

    /**
     * El administrador confirma la llegada de la mercancia: suma el inventario y
     * genera el pago pendiente al proveedor.
     */
    @Transactional
    public SolicitudDTO recibirSolicitud(Integer solicitudId) {
        PedidoInsumo s = solicitud(solicitudId);
        if (!ABIERTAS.contains(s.getEstado())) {
            throw new ReglaNegocioException("La solicitud está " + s.getEstado() + " y ya no se puede recibir");
        }
        List<DetallePedidoInsumo> detalles = detallePedidoInsumoDAO.findByPedidoInsumoId(solicitudId);
        double total = 0;
        for (DetallePedidoInsumo d : detalles) {
            insumoDAO.sumarCantidadDisponible(d.getInsumoId(), d.getCantidad());
            total += (d.getPrecioUnitario() == null ? 0 : d.getPrecioUnitario()) * d.getCantidad();
        }
        s.setEstado(EstadoSolicitud.RECIBIDA);
        s.setFechaRecepcion(LocalDateTime.now());
        pedidoInsumoDAO.save(s);
        generarPago(s.getIdPedidoInsumo(), total);
        return toSolicitudes(List.of(solicitud(solicitudId))).get(0);
    }

    @Transactional
    public SolicitudDTO cancelarSolicitud(Integer solicitudId) {
        PedidoInsumo s = solicitud(solicitudId);
        if (!ABIERTAS.contains(s.getEstado())) {
            throw new ReglaNegocioException("Solo se pueden cancelar solicitudes pendientes o despachadas");
        }
        s.setEstado(EstadoSolicitud.CANCELADA);
        pedidoInsumoDAO.save(s);
        return toSolicitudes(List.of(s)).get(0);
    }

    /** CU-ADM-10 con proveedor: ingreso directo registrado como entrega recibida. */
    @Transactional
    public void registrarEntregaDirecta(UsuarioSesion sesion, Integer proveedorId, Integer insumoId,
                                        Integer cantidad, Double precioUnitario) {
        Integer adminId = administradorId(sesion);
        proveedor(proveedorId);
        InsumoProveedor ip = insumoProveedorDAO.findByProveedorIdAndInsumoId(proveedorId, insumoId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "Ese proveedor no tiene registrado el insumo; vincúlelo primero en Proveedores"));
        double precio = precioUnitario != null ? precioUnitario : (ip.getPrecio() == null ? 0 : ip.getPrecio());

        PedidoInsumo s = new PedidoInsumo();
        s.setAdministradorId(adminId);
        s.setProveedorId(proveedorId);
        s.setFecha(LocalDateTime.now());
        s.setFechaRecepcion(LocalDateTime.now());
        s.setEstado(EstadoSolicitud.RECIBIDA);
        pedidoInsumoDAO.save(s);

        DetallePedidoInsumo d = new DetallePedidoInsumo();
        d.setPedidoInsumoId(s.getIdPedidoInsumo());
        d.setInsumoId(insumoId);
        d.setCantidad(cantidad);
        d.setPrecioUnitario(precio);
        detallePedidoInsumoDAO.save(d);

        insumoDAO.sumarCantidadDisponible(insumoId, cantidad);
        generarPago(s.getIdPedidoInsumo(), precio * cantidad);
    }

    private void generarPago(Integer solicitudId, double monto) {
        PagoProveedor pago = new PagoProveedor();
        pago.setPedidoInsumoId(solicitudId);
        pago.setMonto(CalculadoraCuenta.redondear(monto));
        pago.setFecha(LocalDateTime.now());
        pago.setEstado(EstadoPagoProveedor.PENDIENTE);
        pagoProveedorDAO.save(pago);
    }

    // ------------------------------------------------------------------
    // Pagos a proveedores (RF36)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<PagoProveedorDTO> listarPagos() {
        return toPagos(pagoProveedorDAO.findAllByOrderByFechaDesc());
    }

    @Transactional
    public PagoProveedorDTO pagarProveedor(Integer pagoId) {
        PagoProveedor p = pagoProveedorDAO.findById(pagoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago no encontrado"));
        if (p.getEstado() == EstadoPagoProveedor.PAGADO) {
            throw new ReglaNegocioException("Este pago ya fue registrado como pagado");
        }
        p.setEstado(EstadoPagoProveedor.PAGADO);
        p.setFechaPago(LocalDateTime.now());
        pagoProveedorDAO.save(p);
        return toPagos(List.of(p)).get(0);
    }

    // ------------------------------------------------------------------
    // Portal del proveedor (RF34, RF35, RF36)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ProveedorDTO perfil(UsuarioSesion sesion) {
        return toDTO(deUsuario(sesion));
    }

    @Transactional(readOnly = true)
    public List<SolicitudDTO> solicitudesRecibidas(UsuarioSesion sesion) {
        return toSolicitudes(pedidoInsumoDAO.findByProveedorIdAndEstadoInOrderByFechaDesc(
                deUsuario(sesion).getIdProveedor(), ABIERTAS));
    }

    /** El proveedor confirma que despacho la solicitud. */
    @Transactional
    public SolicitudDTO despacharSolicitud(UsuarioSesion sesion, Integer solicitudId) {
        PedidoInsumo s = solicitud(solicitudId);
        if (!s.getProveedorId().equals(deUsuario(sesion).getIdProveedor())) {
            throw new RecursoNoEncontradoException("Solicitud no encontrada");
        }
        if (s.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new ReglaNegocioException("Solo se pueden despachar solicitudes pendientes");
        }
        s.setEstado(EstadoSolicitud.DESPACHADA);
        pedidoInsumoDAO.save(s);
        return toSolicitudes(List.of(s)).get(0);
    }

    @Transactional(readOnly = true)
    public List<SolicitudDTO> historialEntregas(UsuarioSesion sesion) {
        return toSolicitudes(pedidoInsumoDAO.findByProveedorIdAndEstadoInOrderByFechaDesc(
                deUsuario(sesion).getIdProveedor(), List.of(EstadoSolicitud.RECIBIDA)));
    }

    @Transactional(readOnly = true)
    public ResumenPagosProveedorDTO pagosDelProveedor(UsuarioSesion sesion) {
        List<PagoProveedorDTO> pagos = pagosDeProveedor(deUsuario(sesion).getIdProveedor());
        double pendiente = pagos.stream().filter(p -> p.estado() == EstadoPagoProveedor.PENDIENTE).mapToDouble(PagoProveedorDTO::monto).sum();
        double pagado = pagos.stream().filter(p -> p.estado() == EstadoPagoProveedor.PAGADO).mapToDouble(PagoProveedorDTO::monto).sum();
        return new ResumenPagosProveedorDTO(CalculadoraCuenta.redondear(pendiente), CalculadoraCuenta.redondear(pagado), pagos);
    }

    @Transactional(readOnly = true)
    public List<InsumoSuministradoDTO> catalogo(UsuarioSesion sesion) {
        return perfil(sesion).insumos();
    }

    /** RF34: el proveedor actualiza la disponibilidad y el precio de su catalogo. */
    @Transactional
    public InsumoSuministradoDTO actualizarCatalogo(UsuarioSesion sesion, Integer relacionId, CatalogoRequest req) {
        Proveedor p = deUsuario(sesion);
        InsumoProveedor ip = insumoProveedorDAO.findById(relacionId)
                .filter(x -> x.getProveedorId().equals(p.getIdProveedor()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Insumo no encontrado en su catálogo"));
        ip.setPrecio(CalculadoraCuenta.redondear(req.precio()));
        ip.setDisponible(req.disponible());
        insumoProveedorDAO.save(ip);
        return toDTO(ip, insumosPorId());
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private List<PagoProveedorDTO> pagosDeProveedor(Integer proveedorId) {
        List<Integer> solicitudes = pedidoInsumoDAO.findByProveedorIdAndEstadoInOrderByFechaDesc(
                        proveedorId, List.of(EstadoSolicitud.RECIBIDA)).stream()
                .map(PedidoInsumo::getIdPedidoInsumo).toList();
        if (solicitudes.isEmpty()) return List.of();
        return toPagos(pagoProveedorDAO.findByPedidoInsumoIdIn(solicitudes).stream()
                .sorted(Comparator.comparing(PagoProveedor::getFecha).reversed()).toList());
    }

    private List<PagoProveedorDTO> toPagos(List<PagoProveedor> pagos) {
        Map<Integer, PedidoInsumo> solicitudes = pedidoInsumoDAO.findAllById(
                        pagos.stream().map(PagoProveedor::getPedidoInsumoId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(PedidoInsumo::getIdPedidoInsumo, Function.identity()));
        Map<Integer, String> proveedores = nombresProveedores();
        return pagos.stream().map(p -> {
            PedidoInsumo s = solicitudes.get(p.getPedidoInsumoId());
            Integer provId = s == null ? null : s.getProveedorId();
            return new PagoProveedorDTO(p.getIdPagoProveedor(), p.getPedidoInsumoId(), provId,
                    provId == null ? null : proveedores.get(provId), p.getMonto(), p.getEstado(),
                    p.getFecha(), p.getFechaPago());
        }).toList();
    }

    private List<SolicitudDTO> toSolicitudes(List<PedidoInsumo> solicitudes) {
        if (solicitudes.isEmpty()) return List.of();
        List<Integer> ids = solicitudes.stream().map(PedidoInsumo::getIdPedidoInsumo).toList();
        Map<Integer, List<DetallePedidoInsumo>> detalles = detallePedidoInsumoDAO.findByPedidoInsumoIdIn(ids).stream()
                .collect(Collectors.groupingBy(DetallePedidoInsumo::getPedidoInsumoId));
        Map<Integer, PagoProveedor> pagos = pagoProveedorDAO.findByPedidoInsumoIdIn(ids).stream()
                .collect(Collectors.toMap(PagoProveedor::getPedidoInsumoId, Function.identity(), (a, b) -> a));
        Map<Integer, Insumo> insumos = insumosPorId();
        Map<Integer, String> proveedores = nombresProveedores();

        return solicitudes.stream().map(s -> {
            List<ItemSolicitudDTO> items = detalles.getOrDefault(s.getIdPedidoInsumo(), List.of()).stream().map(d -> {
                Insumo i = insumos.get(d.getInsumoId());
                double precio = d.getPrecioUnitario() == null ? 0 : d.getPrecioUnitario();
                return new ItemSolicitudDTO(d.getInsumoId(), i == null ? null : i.getNombre(),
                        i == null ? null : i.getUnidadMedida(), d.getCantidad(), d.getPrecioUnitario(),
                        CalculadoraCuenta.redondear(precio * d.getCantidad()));
            }).toList();
            double total = items.stream().mapToDouble(ItemSolicitudDTO::subtotal).sum();
            PagoProveedor pago = pagos.get(s.getIdPedidoInsumo());
            return new SolicitudDTO(s.getIdPedidoInsumo(), s.getFecha(), s.getEstado(), s.getProveedorId(),
                    proveedores.get(s.getProveedorId()), items, CalculadoraCuenta.redondear(total),
                    s.getFechaRecepcion(), pago == null ? null : pago.getEstado().name());
        }).toList();
    }

    private Map<Integer, Insumo> insumosPorId() {
        return insumoDAO.findAll().stream().collect(Collectors.toMap(Insumo::getIdInsumo, Function.identity()));
    }

    private Map<Integer, String> nombresProveedores() {
        return Mapas.indexar(proveedorDAO.findAll(), Proveedor::getIdProveedor, Proveedor::getNombre);
    }

    private Integer administradorId(UsuarioSesion sesion) {
        return administradorDAO.findByUsuarioId(sesion.id())
                .orElseThrow(() -> new ReglaNegocioException("El usuario no es administrador"))
                .getIdAdministrador();
    }

    private Proveedor deUsuario(UsuarioSesion sesion) {
        return proveedorDAO.findByUsuarioId(sesion.id())
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un proveedor asociado a su usuario"));
    }

    private Proveedor proveedor(Integer id) {
        return proveedorDAO.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Proveedor no encontrado"));
    }

    private PedidoInsumo solicitud(Integer id) {
        return pedidoInsumoDAO.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada"));
    }
}
