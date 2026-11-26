package com.polirestaurante.config;

import com.polirestaurante.service.observer.CocinaSocket;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@EnableScheduling
public class CocinaWebSocketConfig implements WebSocketConfigurer {
    private final CocinaSocket socket;
    public CocinaWebSocketConfig(CocinaSocket socket) { this.socket = socket; }
    @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Spring mantiene la política de mismo origen por defecto. Autenticación en el primer mensaje.
        registry.addHandler(socket, "/ws/cocina");
    }
}
