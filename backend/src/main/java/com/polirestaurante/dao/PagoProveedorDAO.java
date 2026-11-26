package com.polirestaurante.dao;

import com.polirestaurante.entity.PagoProveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PagoProveedorDAO extends JpaRepository<PagoProveedor, Integer> {
    List<PagoProveedor> findAllByOrderByFechaDesc();
    List<PagoProveedor> findByPedidoInsumoIdIn(Collection<Integer> pedidoInsumoIds);
}
