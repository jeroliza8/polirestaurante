package com.polirestaurante.dao;

import com.polirestaurante.entity.PedidoInsumo;
import com.polirestaurante.enums.EstadoSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PedidoInsumoDAO extends JpaRepository<PedidoInsumo, Integer> {
    List<PedidoInsumo> findAllByOrderByFechaDesc();
    List<PedidoInsumo> findByProveedorIdAndEstadoInOrderByFechaDesc(Integer proveedorId, Collection<EstadoSolicitud> estados);
    boolean existsByAdministradorId(Integer administradorId);
}
