package com.polirestaurante.dao;

import com.polirestaurante.entity.Producto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.*;

/** Acceso a atributos mínimos de auditoría de las tablas compartidas. Sin entidades nuevas. */
@Repository
public class CocinaAuditoriaDAO {
    private final JdbcTemplate jdbc;
    @PersistenceContext private EntityManager em;

    public CocinaAuditoriaDAO(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Optional<Producto> bloquearProducto(Integer id) {
        return Optional.ofNullable(em.find(Producto.class, id, LockModeType.PESSIMISTIC_WRITE));
    }

    public record AuditoriaPedido(Integer usuarioPreparacion, Integer usuarioListo) {}
    public record AuditoriaProducto(LocalDateTime fecha, Integer usuario) {}

    private static String placeholders(int cantidad) {
        return String.join(",", Collections.nCopies(cantidad, "?"));
    }
    public Map<Integer, AuditoriaPedido> pedidos(Collection<Integer> ids) {
        Map<Integer, AuditoriaPedido> resultado = new HashMap<>();
        if (ids.isEmpty()) return resultado;
        jdbc.query("SELECT id_pedido, Usuario_inicio_preparacion, Usuario_listo FROM Pedido WHERE id_pedido IN (" + placeholders(ids.size()) + ")",
            (org.springframework.jdbc.core.RowCallbackHandler) rs -> resultado.put(rs.getInt(1), new AuditoriaPedido(rs.getObject(2, Integer.class), rs.getObject(3, Integer.class))), ids.toArray());
        return resultado;
    }
    public Map<Integer, AuditoriaProducto> productos(Collection<Integer> ids) {
        Map<Integer, AuditoriaProducto> resultado = new HashMap<>();
        if (ids.isEmpty()) return resultado;
        jdbc.query("SELECT idProducto, fecha_disponibilidad, Usuario_disponibilidad FROM Producto WHERE idProducto IN (" + placeholders(ids.size()) + ")",
            (org.springframework.jdbc.core.RowCallbackHandler) rs -> resultado.put(rs.getInt(1), new AuditoriaProducto(rs.getObject(2, LocalDateTime.class), rs.getObject(3, Integer.class))), ids.toArray());
        return resultado;
    }
    public void registrarInicio(Integer pedidoId, Integer usuarioId) {
        jdbc.update("UPDATE Pedido SET Usuario_inicio_preparacion = ? WHERE id_pedido = ?", usuarioId, pedidoId);
    }
    public void registrarListo(Integer pedidoId, Integer usuarioId) {
        jdbc.update("UPDATE Pedido SET Usuario_listo = ? WHERE id_pedido = ?", usuarioId, pedidoId);
    }
    public void registrarDisponibilidad(Integer productoId, Integer usuarioId, LocalDateTime fecha) {
        jdbc.update("UPDATE Producto SET fecha_disponibilidad = ?, Usuario_disponibilidad = ? WHERE idProducto = ?", fecha, usuarioId, productoId);
    }
}
