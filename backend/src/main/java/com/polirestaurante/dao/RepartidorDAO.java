package com.polirestaurante.dao;

import com.polirestaurante.entity.Repartidor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RepartidorDAO extends JpaRepository<Repartidor, Integer> {
    Optional<Repartidor> findByUsuarioId(Integer usuarioId);
}
