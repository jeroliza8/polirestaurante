package com.polirestaurante;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Punto de entrada. En produccion la aplicacion corre DENTRO de Apache Tomcat 10.1
 * (ver {@link ServletInitializer}); este main solo se usa para pruebas locales.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class PoliRestauranteApplication {
    public static void main(String[] args) {
        SpringApplication.run(PoliRestauranteApplication.class, args);
    }
}
