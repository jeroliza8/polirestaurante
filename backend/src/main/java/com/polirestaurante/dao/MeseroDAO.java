package com.polirestaurante.dao;

import com.polirestaurante.entity.Mesero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MeseroDAO extends JpaRepository<Mesero, Integer> {
    Optional<Mesero> findByUsuarioId(Integer usuarioId);
}
