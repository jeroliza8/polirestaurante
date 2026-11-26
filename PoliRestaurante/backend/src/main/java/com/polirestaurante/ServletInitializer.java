package com.polirestaurante;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/** Permite desplegar el WAR en el Apache Tomcat 10.1 del servidor (diagrama de despliegue, ADR-005). */
public class ServletInitializer extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(PoliRestauranteApplication.class);
    }
}
