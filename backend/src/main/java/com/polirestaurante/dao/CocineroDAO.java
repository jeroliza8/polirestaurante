package com.polirestaurante.dao;

import com.polirestaurante.entity.Cocinero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CocineroDAO extends JpaRepository<Cocinero, Integer> {
    Optional<Cocinero> findByUsuarioId(Integer usuarioId);
}
