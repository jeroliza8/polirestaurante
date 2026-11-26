package com.polirestaurante.dao;

import com.polirestaurante.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoDAO extends JpaRepository<Producto, Integer> {
    List<Producto> findByActivoTrueOrderByNombreAsc();
    boolean existsByNombreIgnoreCaseAndActivoTrue(String nombre);
    boolean existsByNombreIgnoreCaseAndActivoTrueAndIdProductoNot(String nombre, Integer idProducto);
    long countByCategoriaIdAndActivoTrue(Integer categoriaId);
    boolean existsByCategoriaId(Integer categoriaId);
}
