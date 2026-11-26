package com.polirestaurante.service;

import com.polirestaurante.dao.*;
import com.polirestaurante.dto.*;
import com.polirestaurante.entity.*;
import com.polirestaurante.enums.EstadoDetalle;
import com.polirestaurante.enums.EstadoPago;
import com.polirestaurante.enums.EstadoPedido;
import com.polirestaurante.exception.*;
import com.polirestaurante.security.UsuarioSesion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Componente ProductoService del diagrama de componentes.
 * Menu y categorias (HU-ADM-01, CU-ADM-04, CU-ADM-05), reportes (CU-ADM-01, CU-ADM-06),
 * inventario (CU-ADM-07, CU-ADM-08) e ingreso de mercancia (CU-ADM-10).
 */
@Service
@RequiredArgsConstructor
public class ProductoService {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ProductoDAO productoDAO;
    private final CategoriaDAO categoriaDAO;
    private final PedidoDAO pedidoDAO;
    private final PedidoMesaDAO pedidoMesaDAO;
    private final MesaDAO mesaDAO;
    private final DetallePedidoDAO detallePedidoDAO;
    private final PagoDAO pagoDAO;
    private final InsumoDAO insumoDAO;
    private final InsumoProveedorDAO insumoProveedorDAO;
    private final ProveedorDAO proveedorDAO;
    private final ProveedorService proveedorService;

    // ------------------------------------------------------------------
    // Productos del menu (HU-ADM-01 / CU-ADM-04)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ProductoDTO> listarProductos() {
        Map<Integer, String> categorias = nombresCategorias();
        return productoDAO.findByActivoTrueOrderByNombreAsc().stream().map(p -> toDTO(p, categorias)).toList();
    }

    @Transactional
    public ProductoDTO crearProducto(ProductoRequest req) {
        String nombre = req.nombre().trim();
        if (productoDAO.existsByNombreIgnoreCaseAndActivoTrue(nombre)) {
            throw new ConflictoException("Ya existe un producto llamado '" + nombre + "' en el menú");
        }
        Producto p = new Producto();
        aplicar(p, req);
        productoDAO.save(p);
        return toDTO(p, nombresCategorias());
    }

    @Transactional
    public ProductoDTO editarProducto(Integer id, ProductoRequest req) {
        Producto p = productoActivo(id);
        if (productoDAO.existsByNombreIgnoreCaseAndActivoTrueAndIdProductoNot(req.nombre().trim(), id)) {
            throw new ConflictoException("Ya existe otro producto llamado '" + req.nombre().trim() + "'");
        }
        aplicar(p, req);
        productoDAO.save(p);
        return toDTO(p, nombresCategorias());
    }

    /** Eliminacion logica: los pedidos historicos siguen referenciando el producto. */
    @Transactional
    public void eliminarProducto(Integer id) {
        Producto p = productoActivo(id);
        p.setActivo(false);
        p.setDisponible(false);
        productoDAO.save(p);
    }

    private void aplicar(Producto p, ProductoRequest req) {
        if (!categoriaDAO.existsById(req.categoriaId())) {
            throw new ReglaNegocioException("La categoría seleccionada no existe");
        }
        p.setNombre(req.nombre().trim());
        p.setDescripcion(req.descripcion() == null ? null : req.descripcion().trim());
        p.setPrecio(CalculadoraCuenta.redondear(req.precio()));
        p.setCantidadDisponible(req.cantidadDisponible());
        p.setCategoriaId(req.categoriaId());
        p.setDisponible(req.disponible() == null || req.disponible());
        p.setStockMinimo(req.stockMinimo() == null ? 5 : req.stockMinimo());
        p.setActivo(true);
    }

    private Producto productoActivo(Integer id) {
        return productoDAO.findById(id).filter(Producto::getActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado"));
    }

    private ProductoDTO toDTO(Producto p, Map<Integer, String> categorias) {
        return new ProductoDTO(p.getIdProducto(), p.getNombre(), p.getDescripcion(), p.getPrecio(),
                p.getCantidadDisponible(), p.getStockMinimo(), Boolean.TRUE.equals(p.getDisponible()),
                p.getCategoriaId(), categorias.get(p.getCategoriaId()),
                estadoStock(p.getCantidadDisponible(), p.getStockMinimo(), Boolean.TRUE.equals(p.getDisponible())));
    }

    // ------------------------------------------------------------------
    // Categorias (RF25 / CU-ADM-05)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<CategoriaDTO> listarCategorias() {
        return categoriaDAO.findAllByOrderByNombreAsc().stream()
                .map(c -> new CategoriaDTO(c.getIdCategoria(), c.getNombre(),
                        productoDAO.countByCategoriaIdAndActivoTrue(c.getIdCategoria())))
                .toList();
    }

    @Transactional
    public CategoriaDTO crearCategoria(String nombreCategoria) {
        String nombre = nombreCategoria.trim();
        if (categoriaDAO.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictoException("Ya existe la categoría '" + nombre + "'");
        }
        Categoria c = new Categoria();
        c.setNombre(nombre);
        categoriaDAO.save(c);
        return new CategoriaDTO(c.getIdCategoria(), c.getNombre(), 0);
    }

    @Transactional
    public CategoriaDTO editarCategoria(Integer id, String nombreCategoria) {
        Categoria c = categoriaDAO.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada"));
        String nombre = nombreCategoria.trim();
        if (categoriaDAO.existsByNombreIgnoreCaseAndIdCategoriaNot(nombre, id)) {
            throw new ConflictoException("Ya existe la categoría '" + nombre + "'");
        }
        c.setNombre(nombre);
        categoriaDAO.save(c);
        return new CategoriaDTO(id, nombre, productoDAO.countByCategoriaIdAndActivoTrue(id));
    }

    @Transactional
    public void eliminarCategoria(Integer id) {
        Categoria c = categoriaDAO.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada"));
        if (productoDAO.existsByCategoriaId(id)) {
            throw new ReglaNegocioException("La categoría tiene productos asociados; reasígnelos antes de eliminarla");
        }
        categoriaDAO.delete(c);
    }

    private Map<Integer, String> nombresCategorias() {
        return Mapas.indexar(categoriaDAO.findAll(), Categoria::getIdCategoria, Categoria::getNombre);
    }

    // ------------------------------------------------------------------
    // CU-ADM-01: reporte de ventas por periodo
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ReporteVentasDTO obtenerReporteVentas(LocalDateTime desde, LocalDateTime hasta, Integer categoriaId) {
        validarRangoFechas(desde, hasta);
        String nombreCategoria = null;
        if (categoriaId != null) {
            nombreCategoria = categoriaDAO.findById(categoriaId).map(Categoria::getNombre)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada"));
        }

        // consultarVentasProductos(fechaIni, fechaFin)
        List<Pago> pagos = pagoDAO.findByEstadoAndFechaPagoBetweenOrderByFechaPagoAsc(EstadoPago.APROBADO, desde, hasta);
        Map<Integer, Pago> pagoPorPedido = pagos.stream()
                .collect(Collectors.toMap(Pago::getPedidoId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        Map<Integer, Producto> productos = productoDAO.findAll().stream()
                .collect(Collectors.toMap(Producto::getIdProducto, Function.identity()));
        Map<Integer, String> categorias = nombresCategorias();

        List<DetallePedido> detalles = pagoPorPedido.isEmpty() ? List.of()
                : detallePedidoDAO.findByPedidoIdIn(pagoPorPedido.keySet()).stream()
                    .filter(d -> d.getEstado() != EstadoDetalle.ANULADO)
                    .filter(d -> categoriaId == null || categoriaId.equals(categoriaDe(d, productos)))
                    .toList();

        // Pedidos y montos que entran al reporte
        Map<Integer, Double> montoPorPedido = new LinkedHashMap<>();
        if (categoriaId == null) {
            pagos.forEach(p -> montoPorPedido.put(p.getPedidoId(), p.getMonto()));
        } else {
            for (DetallePedido d : detalles) {
                montoPorPedido.merge(d.getPedidoId(), subtotal(d, productos), Double::sum);
            }
        }
        if (montoPorPedido.isEmpty()) {
            throw new RecursoNoEncontradoException("No hay resultados para el rango seleccionado");
        }

        double montoTotal = CalculadoraCuenta.redondear(montoPorPedido.values().stream().mapToDouble(Double::doubleValue).sum());
        long numeroPedidos = montoPorPedido.size();
        double descuentos = categoriaId != null ? 0 : pagos.stream().mapToDouble(Pago::getDescuento).sum();

        // Productos mas vendidos
        Map<Integer, long[]> unidades = new HashMap<>();
        Map<Integer, Double> montoProducto = new HashMap<>();
        for (DetallePedido d : detalles) {
            unidades.computeIfAbsent(d.getProductoId(), k -> new long[1])[0] += d.getCantidad();
            montoProducto.merge(d.getProductoId(), subtotal(d, productos), Double::sum);
        }
        List<ItemVendido> top = unidades.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
                .map(e -> new ItemVendido(nombreProducto(e.getKey(), productos), e.getValue()[0],
                        CalculadoraCuenta.redondear(montoProducto.get(e.getKey()))))
                .toList();

        // Por dia
        Map<String, double[]> porDia = new TreeMap<>();
        montoPorPedido.forEach((pedidoId, monto) -> {
            String dia = pagoPorPedido.get(pedidoId).getFechaPago().format(DIA);
            double[] acc = porDia.computeIfAbsent(dia, k -> new double[2]);
            acc[0] += 1;
            acc[1] += monto;
        });
        List<FilaVentaDia> filasDia = porDia.entrySet().stream()
                .map(e -> new FilaVentaDia(e.getKey(), (long) e.getValue()[0], CalculadoraCuenta.redondear(e.getValue()[1])))
                .toList();

        // Por categoria
        Map<String, double[]> porCategoria = new TreeMap<>();
        for (DetallePedido d : detalles) {
            String cat = categorias.getOrDefault(categoriaDe(d, productos), "Sin categoría");
            double[] acc = porCategoria.computeIfAbsent(cat, k -> new double[2]);
            acc[0] += d.getCantidad();
            acc[1] += subtotal(d, productos);
        }
        List<SerieValor> serieCategorias = porCategoria.entrySet().stream()
                .map(e -> new SerieValor(e.getKey(), CalculadoraCuenta.redondear(e.getValue()[1]), (long) e.getValue()[0]))
                .toList();

        // Por medio de pago
        Map<String, double[]> porMedio = new TreeMap<>();
        montoPorPedido.forEach((pedidoId, monto) -> {
            String medio = String.valueOf(pagoPorPedido.get(pedidoId).getMetodoPago());
            double[] acc = porMedio.computeIfAbsent(medio, k -> new double[2]);
            acc[0] += 1;
            acc[1] += monto;
        });
        List<SerieValor> serieMedios = porMedio.entrySet().stream()
                .map(e -> new SerieValor(e.getKey(), CalculadoraCuenta.redondear(e.getValue()[1]), (long) e.getValue()[0]))
                .toList();

        ItemVendido masVendido = top.isEmpty() ? null : top.get(0);
        return new ReporteVentasDTO(desde, hasta, nombreCategoria, montoTotal,
                CalculadoraCuenta.redondear(descuentos), numeroPedidos,
                CalculadoraCuenta.redondear(montoTotal / numeroPedidos),
                masVendido == null ? "-" : masVendido.producto(), masVendido == null ? 0 : masVendido.cantidad(),
                top.stream().limit(10).toList(), filasDia, serieCategorias, serieMedios);
    }

    private Integer categoriaDe(DetallePedido d, Map<Integer, Producto> productos) {
        Producto p = productos.get(d.getProductoId());
        return p == null ? null : p.getCategoriaId();
    }

    private double subtotal(DetallePedido d, Map<Integer, Producto> productos) {
        double precio = d.getPrecioUnitario() != null ? d.getPrecioUnitario()
                : Optional.ofNullable(productos.get(d.getProductoId())).map(Producto::getPrecio).orElse(0.0);
        return precio * d.getCantidad();
    }

    private String nombreProducto(Integer id, Map<Integer, Producto> productos) {
        Producto p = productos.get(id);
        return p == null ? "Producto #" + id : p.getNombre();
    }

    private void validarRangoFechas(LocalDateTime desde, LocalDateTime hasta) {
        if (desde == null || hasta == null) {
            throw new ReglaNegocioException("Indique la fecha inicial y la fecha final");
        }
        if (!hasta.isAfter(desde)) {
            throw new ReglaNegocioException("La fecha final debe ser posterior a la fecha inicial");
        }
    }

    // ------------------------------------------------------------------
    // CU-ADM-06 / RF29: tiempos de preparacion por pedido y por etapa
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ReporteTiemposDTO generarReporteTiempos(LocalDateTime desde, LocalDateTime hasta) {
        validarRangoFechas(desde, hasta);
        List<Pedido> pedidos = pedidoDAO.findByFechaCreacionBetweenAndEstadoNotOrderByFechaCreacionAsc(
                desde, hasta, EstadoPedido.CANCELADO).stream()
                .filter(p -> p.getFechaEnPreparacion() != null)
                .toList();
        if (pedidos.isEmpty()) {
            throw new RecursoNoEncontradoException("No hay resultados para el rango seleccionado");
        }
        Map<Integer, Integer> mesaPorPedido = mesasDePedidos(pedidos.stream().map(Pedido::getIdPedido).toList());

        List<TiempoPedidoDTO> filas = pedidos.stream().map(p -> {
            LocalDateTime fin = p.getFechaEntregado() != null ? p.getFechaEntregado() : p.getFechaListo();
            return new TiempoPedidoDTO(p.getIdPedido(), mesaPorPedido.get(p.getIdPedido()), p.getEstado().name(),
                    p.getFechaCreacion(),
                    minutos(p.getFechaCreacion(), p.getFechaEnPreparacion()),
                    minutos(p.getFechaEnPreparacion(), p.getFechaListo()),
                    minutos(p.getFechaListo(), p.getFechaEntregado()),
                    minutos(p.getFechaCreacion(), fin));
        }).toList();

        return new ReporteTiemposDTO(desde, hasta, filas.size(),
                promedio(filas.stream().map(TiempoPedidoDTO::minutosEspera)),
                promedio(filas.stream().map(TiempoPedidoDTO::minutosPreparacion)),
                promedio(filas.stream().map(TiempoPedidoDTO::minutosEntrega)),
                promedio(filas.stream().map(TiempoPedidoDTO::minutosTotal)),
                filas);
    }

    private Map<Integer, Integer> mesasDePedidos(Collection<Integer> pedidoIds) {
        if (pedidoIds.isEmpty()) return Map.of();
        Map<Integer, Integer> numeroMesa = Mapas.indexar(mesaDAO.findAll(), Mesa::getIdMesa, Mesa::getNumero);
        Map<Integer, Integer> r = new HashMap<>();
        pedidoMesaDAO.findByPedidoIdIn(pedidoIds).forEach(pm -> r.put(pm.getPedidoId(), numeroMesa.get(pm.getMesaId())));
        return r;
    }

    private Double minutos(LocalDateTime a, LocalDateTime b) {
        if (a == null || b == null || b.isBefore(a)) return null;
        return CalculadoraCuenta.redondear(Duration.between(a, b).toSeconds() / 60.0);
    }

    private Double promedio(java.util.stream.Stream<Double> valores) {
        OptionalDouble avg = valores.filter(Objects::nonNull).mapToDouble(Double::doubleValue).average();
        return avg.isPresent() ? CalculadoraCuenta.redondear(avg.getAsDouble()) : null;
    }

    // ------------------------------------------------------------------
    // CU-ADM-07 / RF30: inventario    CU-ADM-08 / RF31: agotados
    // ------------------------------------------------------------------

    /** obtenerDisponibilidadInsumos() del diagrama de secuencia. */
    @Transactional(readOnly = true)
    public InventarioDTO obtenerDisponibilidadInsumos() {
        return new InventarioDTO(listarInsumos(), listarProductos());
    }

    @Transactional(readOnly = true)
    public ReporteAgotadosDTO generarReporteAgotados() {
        List<InsumoDTO> insumos = listarInsumos().stream().filter(i -> !"OK".equals(i.estado())).toList();
        List<ProductoDTO> productos = listarProductos().stream().filter(p -> !"OK".equals(p.estadoStock())).toList();
        return new ReporteAgotadosDTO(LocalDate.now(), insumos, productos, insumos.size() + productos.size());
    }

    @Transactional(readOnly = true)
    public List<InsumoDTO> listarInsumos() {
        Map<Integer, String> proveedores = Mapas.indexar(proveedorDAO.findAll(), Proveedor::getIdProveedor, Proveedor::getNombre);
        Map<Integer, List<String>> provPorInsumo = new HashMap<>();
        insumoProveedorDAO.findAll().forEach(ip -> provPorInsumo
                .computeIfAbsent(ip.getInsumoId(), k -> new ArrayList<>())
                .add(proveedores.get(ip.getProveedorId())));
        return insumoDAO.findAllByOrderByNombreAsc().stream()
                .map(i -> toDTO(i, provPorInsumo.getOrDefault(i.getIdInsumo(), List.of())))
                .toList();
    }

    @Transactional
    public InsumoDTO crearInsumo(InsumoRequest req) {
        String nombre = req.nombre().trim();
        if (insumoDAO.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictoException("Ya existe el insumo '" + nombre + "'");
        }
        Insumo i = new Insumo();
        i.setNombre(nombre);
        i.setUnidadMedida(req.unidadMedida().trim());
        i.setCantidadDisponible(req.cantidadDisponible());
        i.setStockMinimo(req.stockMinimo());
        insumoDAO.save(i);
        return toDTO(i, List.of());
    }

    @Transactional
    public InsumoDTO editarInsumo(Integer id, InsumoRequest req) {
        Insumo i = insumo(id);
        if (insumoDAO.existsByNombreIgnoreCaseAndIdInsumoNot(req.nombre().trim(), id)) {
            throw new ConflictoException("Ya existe el insumo '" + req.nombre().trim() + "'");
        }
        i.setNombre(req.nombre().trim());
        i.setUnidadMedida(req.unidadMedida().trim());
        i.setCantidadDisponible(req.cantidadDisponible());
        i.setStockMinimo(req.stockMinimo());
        insumoDAO.save(i);
        return buscarInsumoDTO(id);
    }

    /**
     * CU-ADM-10 / RF33: registrar el ingreso de mercancia y actualizar el inventario.
     * Con proveedor, el ingreso queda como entrega recibida de ese proveedor.
     */
    @Transactional
    public InsumoDTO registrarIngresoMercancia(Integer insumoId, IngresoMercanciaRequest req, UsuarioSesion sesion) {
        insumo(insumoId);
        if (req.proveedorId() != null) {
            proveedorService.registrarEntregaDirecta(sesion, req.proveedorId(), insumoId, req.cantidad(), req.precioUnitario());
        } else {
            insumoDAO.sumarCantidadDisponible(insumoId, req.cantidad());   // sumarCantidadDisponible(idInsumo, cantidad)
        }
        return buscarInsumoDTO(insumoId);
    }

    private InsumoDTO buscarInsumoDTO(Integer id) {
        return listarInsumos().stream().filter(x -> x.id().equals(id)).findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Insumo no encontrado"));
    }

    private Insumo insumo(Integer id) {
        return insumoDAO.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Insumo no encontrado"));
    }

    private InsumoDTO toDTO(Insumo i, List<String> proveedores) {
        return new InsumoDTO(i.getIdInsumo(), i.getNombre(), i.getUnidadMedida(), i.getCantidadDisponible(),
                i.getStockMinimo(), estadoStock(i.getCantidadDisponible(), i.getStockMinimo(), true), proveedores);
    }

    static String estadoStock(Integer cantidad, Integer minimo, boolean disponible) {
        int c = cantidad == null ? 0 : cantidad;
        if (!disponible || c <= 0) return "AGOTADO";
        if (minimo != null && c <= minimo) return "BAJO";
        return "OK";
    }
}
