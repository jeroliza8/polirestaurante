package com.polirestaurante.service;

import com.polirestaurante.dao.*;
import com.polirestaurante.dto.*;
import com.polirestaurante.entity.*;
import com.polirestaurante.enums.EstadoMesa;
import com.polirestaurante.enums.EstadoPedido;
import com.polirestaurante.enums.EstadoUsuario;
import com.polirestaurante.enums.Rol;
import com.polirestaurante.exception.*;
import com.polirestaurante.security.JwtUtil;
import com.polirestaurante.security.UsuarioSesion;
import com.polirestaurante.service.factory.EmpleadoFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Componente UsuarioService del diagrama de componentes.
 * Autenticacion (RF37, RF38), usuarios y roles (HU-ADM-02, CU-ADM-03) y,
 * segun el diagrama de secuencia CU-ADM-02, la administracion de mesas.
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioDAO usuarioDAO;
    private final AdministradorDAO administradorDAO;
    private final CajeroDAO cajeroDAO;
    private final MeseroDAO meseroDAO;
    private final CocineroDAO cocineroDAO;
    private final RepartidorDAO repartidorDAO;
    private final ProveedorDAO proveedorDAO;
    private final MesaDAO mesaDAO;
    private final PedidoMesaDAO pedidoMesaDAO;
    private final PedidoDAO pedidoDAO;
    private final CajaDAO cajaDAO;
    private final PagoDAO pagoDAO;
    private final PedidoInsumoDAO pedidoInsumoDAO;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmpleadoFactory empleadoFactory;

    // ------------------------------------------------------------------
    // Autenticacion
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public LoginResponse autenticar(LoginRequest req) {
        Usuario u = usuarioDAO.findByNombre(req.usuario().trim())
                .filter(x -> x.getCredenciales() != null && passwordEncoder.matches(req.password(), x.getCredenciales()))
                .orElseThrow(() -> new AutenticacionException("Usuario o contraseña incorrectos"));
        if (u.getEstado() != EstadoUsuario.activo) {
            throw new AutenticacionException("Su cuenta está desactivada. Contacte al administrador.");
        }
        Rol rol = detectarRol(u.getIdUsuario())
                .orElseThrow(() -> new AutenticacionException("El usuario no tiene un rol asignado"));
        return emitirToken(new UsuarioSesion(u.getIdUsuario(), u.getNombre(), rol));
    }

    /** Renueva el token mientras el usuario esta activo en la pantalla (RNF04: 30 min de inactividad). */
    public LoginResponse renovar(UsuarioSesion sesion) {
        return emitirToken(sesion);
    }

    private LoginResponse emitirToken(UsuarioSesion s) {
        return new LoginResponse(jwtUtil.generar(s), s.id(), s.nombre(), s.rol(), jwtUtil.getExpiracionSegundos());
    }

    public Optional<Rol> detectarRol(Integer usuarioId) {
        if (administradorDAO.findByUsuarioId(usuarioId).isPresent()) return Optional.of(Rol.ADMINISTRADOR);
        if (cajeroDAO.findByUsuarioId(usuarioId).isPresent()) return Optional.of(Rol.CAJERO);
        if (meseroDAO.findByUsuarioId(usuarioId).isPresent()) return Optional.of(Rol.MESERO);
        if (cocineroDAO.findByUsuarioId(usuarioId).isPresent()) return Optional.of(Rol.COCINERO);
        if (repartidorDAO.findByUsuarioId(usuarioId).isPresent()) return Optional.of(Rol.REPARTIDOR);
        if (proveedorDAO.findByUsuarioId(usuarioId).isPresent()) return Optional.of(Rol.PROVEEDOR);
        return Optional.empty();
    }

    // ------------------------------------------------------------------
    // Usuarios y roles (HU-ADM-02, CU-ADM-03)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<UsuarioDTO> listarUsuarios() {
        Map<Integer, String> detalle = new HashMap<>();
        Map<Integer, Rol> roles = new HashMap<>();
        administradorDAO.findAll().forEach(a -> roles.put(a.getUsuarioId(), Rol.ADMINISTRADOR));
        cajeroDAO.findAll().forEach(c -> roles.put(c.getUsuarioId(), Rol.CAJERO));
        meseroDAO.findAll().forEach(m -> { roles.put(m.getUsuarioId(), Rol.MESERO); detalle.put(m.getUsuarioId(), m.getZonaAsignada()); });
        cocineroDAO.findAll().forEach(c -> { roles.put(c.getUsuarioId(), Rol.COCINERO); detalle.put(c.getUsuarioId(), c.getEstacion()); });
        repartidorDAO.findAll().forEach(r -> { roles.put(r.getUsuarioId(), Rol.REPARTIDOR); detalle.put(r.getUsuarioId(), r.getPlacaVehiculo()); });
        proveedorDAO.findAll().stream().filter(p -> p.getUsuarioId() != null)
                .forEach(p -> { roles.put(p.getUsuarioId(), Rol.PROVEEDOR); detalle.put(p.getUsuarioId(), p.getNombre()); });
        return usuarioDAO.findAll().stream()
                .sorted(Comparator.comparing(Usuario::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(u -> new UsuarioDTO(u.getIdUsuario(), u.getNombre(), roles.get(u.getIdUsuario()),
                        u.getEstado(), detalle.get(u.getIdUsuario())))
                .toList();
    }

    /** Roles del sistema y lo que puede hacer cada uno (control de acceso de SecurityConfig). */
    public List<RolDTO> listarRoles() {
        return List.of(
            new RolDTO(Rol.ADMINISTRADOR, "Gestiona el sistema",
                List.of("Menú y categorías", "Mesas", "Usuarios y roles", "Reportes", "Inventario", "Proveedores")),
            new RolDTO(Rol.CAJERO, "Gestiona caja, cuentas y pagos",
                List.of("Apertura y cierre de caja", "Cuentas", "Anular productos", "Promociones", "Pagos y facturas")),
            new RolDTO(Rol.MESERO, "Toma y entrega pedidos", List.of("Pedidos", "Estado de mesas")),
            new RolDTO(Rol.COCINERO, "Prepara pedidos", List.of("Tablero de cocina", "Disponibilidad de productos")),
            new RolDTO(Rol.REPARTIDOR, "Entrega domicilios", List.of("Pedidos asignados", "Estado de entrega")),
            new RolDTO(Rol.PROVEEDOR, "Suministra insumos",
                List.of("Solicitudes recibidas", "Historial de entregas", "Pagos pendientes", "Catálogo")));
    }

    @Transactional
    public UsuarioDTO registrarEmpleado(CrearUsuarioRequest req) {
        String nombre = req.nombre().trim();
        if (usuarioDAO.existsByNombre(nombre)) {
            throw new ConflictoException("Ya existe un usuario con el nombre '" + nombre + "'");
        }
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setCredenciales(passwordEncoder.encode(req.password()));
        u.setEstado(EstadoUsuario.activo);
        usuarioDAO.save(u);

        guardarEmpleado(empleadoFactory.crear(req.rol(), u.getIdUsuario(), req.datosRol()));
        return buscarUsuarioDTO(u.getIdUsuario());
    }

    /** HU-ADM-02 AC2: asigna un rol existente a un usuario. */
    @Transactional
    public UsuarioDTO cambiarRol(Integer usuarioId, CambioRolRequest req, UsuarioSesion sesion) {
        Usuario u = usuario(usuarioId);
        if (usuarioId.equals(sesion.id())) {
            throw new ReglaNegocioException("No es posible cambiar el rol de su propia cuenta");
        }
        Optional<Rol> actual = detectarRol(usuarioId);
        if (actual.isPresent() && actual.get() == Rol.PROVEEDOR) {
            throw new ReglaNegocioException("El usuario pertenece a un proveedor; adminístrelo desde Proveedores");
        }
        if (actual.isPresent() && actual.get() == req.rol()) {
            throw new ReglaNegocioException("El usuario ya tiene el rol " + req.rol());
        }
        actual.ifPresent(r -> quitarRol(usuarioId, r));
        guardarEmpleado(empleadoFactory.crear(req.rol(), u.getIdUsuario(), req.datosRol()));
        return buscarUsuarioDTO(usuarioId);
    }

    /** CU-ADM-03: desactivar o reactivar (actualizarEstadoUsuario del diagrama de secuencia). */
    @Transactional
    public UsuarioDTO cambiarEstadoUsuario(Integer usuarioId, boolean activo, UsuarioSesion sesion) {
        Usuario u = usuario(usuarioId);
        if (!activo && usuarioId.equals(sesion.id())) {
            throw new ReglaNegocioException("No es posible desactivar su propia cuenta");
        }
        u.setEstado(activo ? EstadoUsuario.activo : EstadoUsuario.inactivo);
        usuarioDAO.save(u);
        return buscarUsuarioDTO(usuarioId);
    }

    @Transactional
    public void restablecerPassword(Integer usuarioId, String password) {
        Usuario u = usuario(usuarioId);
        u.setCredenciales(passwordEncoder.encode(password));
        usuarioDAO.save(u);
    }

    private void guardarEmpleado(Empleado empleado) {
        switch (empleado) {
            case Administrador a -> administradorDAO.save(a);
            case Cajero c -> cajeroDAO.save(c);
            case Mesero m -> meseroDAO.save(m);
            case Cocinero c -> cocineroDAO.save(c);
            case Repartidor r -> repartidorDAO.save(r);
            default -> throw new IllegalArgumentException("Tipo de empleado no soportado");
        }
    }

    /**
     * Quita el rol anterior. Si el usuario ya tiene historial con ese rol (cajas, pagos,
     * pedidos, solicitudes) no se borra, para no perder la trazabilidad.
     */
    private void quitarRol(Integer usuarioId, Rol rol) {
        String conHistorial = "El usuario tiene historial como " + rol
                + "; para conservar la trazabilidad desactívelo y cree un usuario nuevo con el otro rol";
        switch (rol) {
            case ADMINISTRADOR -> administradorDAO.findByUsuarioId(usuarioId).ifPresent(a -> {
                if (pedidoInsumoDAO.existsByAdministradorId(a.getIdAdministrador())) throw new ReglaNegocioException(conHistorial);
                administradorDAO.delete(a);
            });
            case CAJERO -> cajeroDAO.findByUsuarioId(usuarioId).ifPresent(c -> {
                if (cajaDAO.existsByCajeroId(c.getIdCajero()) || pagoDAO.existsByCajeroId(c.getIdCajero()))
                    throw new ReglaNegocioException(conHistorial);
                cajeroDAO.delete(c);
            });
            case MESERO -> meseroDAO.findByUsuarioId(usuarioId).ifPresent(m -> {
                if (pedidoDAO.existsByMeseroId(m.getIdMesero())) throw new ReglaNegocioException(conHistorial);
                meseroDAO.delete(m);
            });
            case COCINERO -> cocineroDAO.findByUsuarioId(usuarioId).ifPresent(cocineroDAO::delete);
            case REPARTIDOR -> repartidorDAO.findByUsuarioId(usuarioId).ifPresent(r -> {
                if (pedidoDAO.existsByRepartidorId(r.getIdRepartidor())) throw new ReglaNegocioException(conHistorial);
                repartidorDAO.delete(r);
            });
            case PROVEEDOR -> throw new ReglaNegocioException("El rol de proveedor se administra desde Proveedores");
        }
    }

    private UsuarioDTO buscarUsuarioDTO(Integer usuarioId) {
        return listarUsuarios().stream().filter(x -> x.id().equals(usuarioId)).findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    private Usuario usuario(Integer id) {
        return usuarioDAO.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    // ------------------------------------------------------------------
    // Mesas (CU-ADM-02) - en el diagrama de secuencia las atiende UsuarioService
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<MesaDTO> listarMesas() {
        Set<Integer> conPedido = new HashSet<>(pedidoMesaDAO.mesasConPedidoActivo(EstadoPedido.CERRADOS));
        return mesaDAO.findByActivaTrueOrderByNumeroAsc().stream()
                .map(m -> toDTO(m, conPedido.contains(m.getIdMesa())))
                .toList();
    }

    /**
     * Si el numero ya existe (o existio: los numeros de mesas eliminadas no se reasignan)
     * responde 409 con el siguiente numero disponible para que el administrador confirme.
     */
    @Transactional
    public MesaDTO registrarMesa(MesaRequest req) {
        if (mesaDAO.existsByNumero(req.numero())) {
            throw new ConflictoException("Ya existe o existió una mesa con el número " + req.numero(),
                    Map.of("sugerencia", mesaDAO.maximoNumero() + 1));
        }
        Mesa m = new Mesa();
        m.setNumero(req.numero());
        m.setCapacidad(req.capacidad());
        m.setEstado(EstadoMesa.LIBRE.name());
        m.setActiva(true);
        return toDTO(mesaDAO.save(m), false);
    }

    @Transactional
    public MesaDTO editarMesa(Integer id, MesaRequest req) {
        Mesa m = mesaActiva(id);
        if (mesaDAO.existsByNumeroAndIdMesaNot(req.numero(), id)) {
            throw new ConflictoException("Ya existe o existió una mesa con el número " + req.numero(),
                    Map.of("sugerencia", mesaDAO.maximoNumero() + 1));
        }
        m.setNumero(req.numero());
        m.setCapacidad(req.capacidad());
        mesaDAO.save(m);
        return toDTO(m, pedidoMesaDAO.contarPedidosActivos(id, EstadoPedido.CERRADOS) > 0);
    }

    /** Eliminacion logica: la mesa sale del listado y su numero no se reasigna. */
    @Transactional
    public void eliminarMesa(Integer id) {
        Mesa m = mesaActiva(id);
        if (pedidoMesaDAO.contarPedidosActivos(id, EstadoPedido.CERRADOS) > 0) {
            throw new ReglaNegocioException("No se puede eliminar una mesa con un pedido en curso");
        }
        m.setActiva(false);
        mesaDAO.save(m);
    }

    private Mesa mesaActiva(Integer id) {
        return mesaDAO.findById(id).filter(Mesa::getActiva)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mesa no encontrada"));
    }

    private MesaDTO toDTO(Mesa m, boolean pedidoActivo) {
        return new MesaDTO(m.getIdMesa(), m.getNumero(), m.getCapacidad(),
                m.getEstado() == null ? EstadoMesa.LIBRE.name() : m.getEstado().toUpperCase(), pedidoActivo);
    }
}
