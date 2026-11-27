package com.polirestaurante.dao;

import com.polirestaurante.entity.Mesa;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MesaDAO extends JpaRepository<Mesa, Integer> {
    List<Mesa> findByActivaTrueOrderByNumeroAsc();
    /** Incluye mesas eliminadas: su numero no se reasigna (CU-ADM-02). */
    boolean existsByNumero(Integer numero);
    boolean existsByNumeroAndIdMesaNot(Integer numero, Integer idMesa);

    @Query("select coalesce(max(m.numero), 0) from Mesa m")
    Integer maximoNumero();

    /** Mesero (RF01/RF06): serializa a dos meseros que intentan abrir la misma mesa. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Mesa m where m.idMesa = :id")
    Optional<Mesa> bloquearPorId(@Param("id") Integer id);
}
