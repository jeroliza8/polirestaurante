package com.polirestaurante.dao;

import com.polirestaurante.entity.DetallePedidoInsumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface DetallePedidoInsumoDAO extends JpaRepository<DetallePedidoInsumo, Integer> {
    List<DetallePedidoInsumo> findByPedidoInsumoId(Integer pedidoInsumoId);
    List<DetallePedidoInsumo> findByPedidoInsumoIdIn(Collection<Integer> pedidoInsumoIds);
}
