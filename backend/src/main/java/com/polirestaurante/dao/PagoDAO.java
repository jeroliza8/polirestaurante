package com.polirestaurante.dao;

import com.polirestaurante.entity.Pago;
import com.polirestaurante.enums.EstadoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PagoDAO extends JpaRepository<Pago, Integer> {
    List<Pago> findByEstadoAndFechaPagoBetweenOrderByFechaPagoAsc(EstadoPago estado, LocalDateTime desde, LocalDateTime hasta);
    List<Pago> findByFechaPagoBetweenOrderByFechaPagoDesc(LocalDateTime desde, LocalDateTime hasta);
    List<Pago> findByCajaIdAndEstado(Integer cajaId, EstadoPago estado);
    boolean existsByPedidoIdAndEstado(Integer pedidoId, EstadoPago estado);
    boolean existsByCajeroId(Integer cajeroId);
}
