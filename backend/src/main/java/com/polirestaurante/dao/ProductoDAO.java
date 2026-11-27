package com.polirestaurante.dao;

import com.polirestaurante.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoDAO extends JpaRepository<Producto, Integer> {
    List<Producto> findByActivoTrueOrderByNombreAsc();
    boolean existsByNombreIgnoreCaseAndActivoTrue(String nombre);
    boolean existsByNombreIgnoreCaseAndActivoTrueAndIdProductoNot(String nombre, Integer idProducto);
    long countByCategoriaIdAndActivoTrue(Integer categoriaId);
    boolean existsByCategoriaId(Integer categoriaId);

    /** Mesero (RF01/RF02): descuenta existencias de forma atómica; devuelve 0 si no alcanzan. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Producto p set p.cantidadDisponible = p.cantidadDisponible - :cantidad "
         + "where p.idProducto = :id and p.activo = true and p.disponible = true and p.cantidadDisponible >= :cantidad")
    int reservar(@Param("id") Integer id, @Param("cantidad") int cantidad);

    /** Mesero (RF02/RF04): devuelve existencias al retirar un producto o cancelar un pedido pendiente. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Producto p set p.cantidadDisponible = p.cantidadDisponible + :cantidad where p.idProducto = :id")
    int devolver(@Param("id") Integer id, @Param("cantidad") int cantidad);
}
