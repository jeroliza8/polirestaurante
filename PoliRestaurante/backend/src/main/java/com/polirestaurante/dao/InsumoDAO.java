package com.polirestaurante.dao;

import com.polirestaurante.entity.Insumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InsumoDAO extends JpaRepository<Insumo, Integer> {
    List<Insumo> findAllByOrderByNombreAsc();
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdInsumoNot(String nombre, Integer idInsumo);

    /** CU-ADM-10: suma atomica en la base, sin condiciones de carrera entre dos ingresos simultaneos. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Insumo i set i.cantidadDisponible = coalesce(i.cantidadDisponible, 0) + :cantidad where i.idInsumo = :id")
    int sumarCantidadDisponible(@Param("id") Integer id, @Param("cantidad") Integer cantidad);
}
