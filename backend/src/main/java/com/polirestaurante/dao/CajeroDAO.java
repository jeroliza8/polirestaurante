package com.polirestaurante.dao;

import com.polirestaurante.entity.Cajero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CajeroDAO extends JpaRepository<Cajero, Integer> {
    Optional<Cajero> findByUsuarioId(Integer usuarioId);
}
