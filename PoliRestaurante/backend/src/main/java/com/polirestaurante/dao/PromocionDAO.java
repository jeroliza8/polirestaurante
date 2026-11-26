package com.polirestaurante.dao;

import com.polirestaurante.config.PoliRestauranteProperties;
import com.polirestaurante.entity.Promocion;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DAO de promociones del diagrama de secuencia CU-CAJ-002.
 * Lee el catalogo definido en la configuracion (no hay tabla de promociones).
 */
@Repository
public class PromocionDAO {

    private final PoliRestauranteProperties props;

    public PromocionDAO(PoliRestauranteProperties props) {
        this.props = props;
    }

    public List<Promocion> listar() {
        return List.copyOf(props.getPromociones());
    }

    /** Paso 3-4 del diagrama: buscarPromocionesVigentes(fechaActual). */
    public List<Promocion> buscarPromocionesVigentes(LocalDateTime fechaActual) {
        return props.getPromociones().stream().filter(p -> p.vigente(fechaActual)).toList();
    }

    public Optional<Promocion> buscarPorCodigo(String codigo) {
        if (codigo == null) return Optional.empty();
        return props.getPromociones().stream()
                .filter(p -> codigo.equalsIgnoreCase(p.getCodigo()))
                .findFirst();
    }
}
