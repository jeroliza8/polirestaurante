package com.polirestaurante.dao;

import com.polirestaurante.entity.Caja;
import com.polirestaurante.enums.EstadoCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CajaDAO extends JpaRepository<Caja, Integer> {
    Optional<Caja> findFirstByCajeroIdAndEstadoOrderByIdCajaDesc(Integer cajeroId, EstadoCaja estado);
    List<Caja> findTop10ByCajeroIdOrderByIdCajaDesc(Integer cajeroId);
    boolean existsByCajeroId(Integer cajeroId);
}
