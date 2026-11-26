package com.polirestaurante.dao;

import com.polirestaurante.entity.Administrador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdministradorDAO extends JpaRepository<Administrador, Integer> {
    Optional<Administrador> findByUsuarioId(Integer usuarioId);
}
