package com.polirestaurante.dao;

import com.polirestaurante.entity.Pedido;
import com.polirestaurante.enums.EstadoPedido;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoDAO extends JpaRepository<Pedido, Integer> {

    /** RF12 / RF13: cuentas abiertas de todos los meseros y cajeros. */
    List<Pedido> findByEstadoNotInOrderByFechaCreacionAsc(Collection<EstadoPedido> estados);

    List<Pedido> findByFechaCreacionBetweenAndEstadoNotOrderByFechaCreacionAsc(
            LocalDateTime desde, LocalDateTime hasta, EstadoPedido excluido);

    /**
     * Bloquea la fila del pedido durante el cobro: dos cajeros no pueden cobrar la
     * misma cuenta a la vez y un reintento no duplica el pago (RNF11, idempotencia).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.idPedido = :id")
    Optional<Pedido> bloquearPorId(@Param("id") Integer id);

    boolean existsByMeseroId(Integer meseroId);
    boolean existsByRepartidorId(Integer repartidorId);
}
