package com.polirestaurante.dao;

import com.polirestaurante.entity.DetallePedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/** En los diagramas de secuencia aparece como CuentaDAO (productos de una cuenta). */
@Repository
public interface DetallePedidoDAO extends JpaRepository<DetallePedido, Integer> {
    List<DetallePedido> findByPedidoIdOrderByIdDetallePedidoAsc(Integer pedidoId);
    List<DetallePedido> findByPedidoIdIn(Collection<Integer> pedidoIds);
}
