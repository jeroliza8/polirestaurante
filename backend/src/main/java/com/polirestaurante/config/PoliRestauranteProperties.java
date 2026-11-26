package com.polirestaurante.config;

import com.polirestaurante.entity.Promocion;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Parametros del sistema leidos de application.yml / variables de entorno (RNF15).
 * Las promociones viven aqui porque el modelo de datos no tiene tabla de promociones.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "polirestaurante")
public class PoliRestauranteProperties {

    private Jwt jwt = new Jwt();
    private Impuesto impuesto = new Impuesto();
    private List<Promocion> promociones = new ArrayList<>();

    @Getter
    @Setter
    public static class Jwt {
        /** Clave HMAC de al menos 32 caracteres. En produccion va en la variable JWT_SECRET. */
        private String secret;
        /** RNF04: la sesion inactiva se cierra a los 30 minutos. */
        private int expiracionMinutos = 30;
    }

    @Getter
    @Setter
    public static class Impuesto {
        private String nombre = "Impoconsumo";
        /** Porcentaje incluido en el precio de venta (en Colombia, impoconsumo de restaurantes: 8 %). */
        private double porcentaje = 8;
    }
}
