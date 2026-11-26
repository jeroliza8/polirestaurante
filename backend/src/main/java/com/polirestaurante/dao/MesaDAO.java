package com.polirestaurante.dao;

import com.polirestaurante.entity.Mesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MesaDAO extends JpaRepository<Mesa, Integer> {
    List<Mesa> findByActivaTrueOrderByNumeroAsc();
    /** Incluye mesas eliminadas: su numero no se reasigna (CU-ADM-02). */
    boolean existsByNumero(Integer numero);
    boolean existsByNumeroAndIdMesaNot(Integer numero, Integer idMesa);

    @Query("select coalesce(max(m.numero), 0) from Mesa m")
    Integer maximoNumero();
}
