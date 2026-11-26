package com.polirestaurante.dao;

import com.polirestaurante.entity.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProveedorDAO extends JpaRepository<Proveedor, Integer> {
    Optional<Proveedor> findByUsuarioId(Integer usuarioId);
    List<Proveedor> findAllByOrderByNombreAsc();
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdProveedorNot(String nombre, Integer idProveedor);
}
