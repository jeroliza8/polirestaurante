package com.polirestaurante.dao;

import com.polirestaurante.entity.InsumoProveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InsumoProveedorDAO extends JpaRepository<InsumoProveedor, Integer> {
    Optional<InsumoProveedor> findByProveedorIdAndInsumoId(Integer proveedorId, Integer insumoId);
    List<InsumoProveedor> findByProveedorId(Integer proveedorId);
}
