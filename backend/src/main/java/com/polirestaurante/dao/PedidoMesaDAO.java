package com.polirestaurante.dao;

import com.polirestaurante.entity.PedidoMesa;
import com.polirestaurante.enums.EstadoPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PedidoMesaDAO extends JpaRepository<PedidoMesa, Integer> {
    List<PedidoMesa> findByPedidoIdIn(Collection<Integer> pedidoIds);

    @Query("select count(p) from PedidoMesa pm, Pedido p "
         + "where p.idPedido = pm.pedidoId and pm.mesaId = :mesaId and p.estado not in :cerrados")
    long contarPedidosActivos(@Param("mesaId") Integer mesaId,
                              @Param("cerrados") Collection<EstadoPedido> cerrados);

    @Query("select distinct pm.mesaId from PedidoMesa pm, Pedido p "
         + "where p.idPedido = pm.pedidoId and p.estado not in :cerrados")
    List<Integer> mesasConPedidoActivo(@Param("cerrados") Collection<EstadoPedido> cerrados);
}
