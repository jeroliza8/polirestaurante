package com.polirestaurante.service.observer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.polirestaurante.enums.Rol;
import com.polirestaurante.security.AccesoCocina;
import com.polirestaurante.security.JwtUtil;
import com.polirestaurante.security.UsuarioSesion;
import com.polirestaurante.service.CocineroService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/** Observer + ADR-001. No envía datos antes de autenticar el JWT; no usa tokens en URL. */
@Component
public class CocinaSocket extends TextWebSocketHandler implements ObservadorPedido {
    private static final Logger log = LoggerFactory.getLogger(CocinaSocket.class);
    private final JwtUtil jwt;
    private final AccesoCocina acceso;
    private final CocineroService cocina;
    private final ObjectMapper json;
    private final Map<String, Conexion> conexiones = new ConcurrentHashMap<>();
    private volatile String huellaAnterior;
    private final LinkedBlockingQueue<EventoCocina> eventosPendientes = new LinkedBlockingQueue<>(4096);

    private static final class Conexion {
        final WebSocketSession socket;
        final Instant abierta = Instant.now();
        volatile String token;
        volatile UsuarioSesion usuario;
        volatile Integer meseroId;
        Conexion(WebSocketSession s) { socket = new ConcurrentWebSocketSessionDecorator(s, 5000, 65536); }
    }
    public CocinaSocket(JwtUtil jwt, AccesoCocina acceso, CocineroService cocina, ObjectMapper json) {
        this.jwt = jwt; this.acceso = acceso; this.cocina = cocina; this.json = json;
    }
    @Override public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (conexiones.size() >= 80) { session.close(CloseStatus.POLICY_VIOLATION); return; }
        session.setTextMessageSizeLimit(8192);
        conexiones.put(session.getId(), new Conexion(session));
    }
    @Override protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Conexion c = conexiones.get(session.getId());
        if (c == null) return;
        try {
            var nodo = json.readTree(message.getPayload());
            if (!"AUTENTICAR".equals(nodo.path("tipo").asText())) { cerrar(c, CloseStatus.POLICY_VIOLATION); return; }
            String token = nodo.path("token").asText("");
            UsuarioSesion usuario = jwt.validar(token);
            if (!acceso.vigente(usuario)) { cerrar(c, CloseStatus.POLICY_VIOLATION); return; }
            // Permite renovar el JWT sin desconectar, pero jamás cambiar de usuario/rol.
            if (c.usuario != null && (!c.usuario.id().equals(usuario.id()) || c.usuario.rol() != usuario.rol())) {
                cerrar(c, CloseStatus.POLICY_VIOLATION); return;
            }
            c.meseroId = acceso.meseroId(usuario);
            c.token = token;
            c.usuario = usuario;
            enviar(c, Map.of("tipo", "CONECTADO"));
        } catch (Exception ex) { cerrar(c, CloseStatus.POLICY_VIOLATION); }
    }
    @Override public void afterConnectionClosed(WebSocketSession session, CloseStatus status) { conexiones.remove(session.getId()); }
    @Override public void handleTransportError(WebSocketSession session, Throwable error) {
        Conexion c = conexiones.get(session.getId());
        if (c != null) cerrar(c, CloseStatus.SERVER_ERROR);
    }
    private boolean autorizada(Conexion c) {
        if (!c.socket.isOpen()) { cerrar(c, CloseStatus.NORMAL); return false; }
        if (c.usuario == null) {
            if (Instant.now().isAfter(c.abierta.plusSeconds(10))) cerrar(c, CloseStatus.POLICY_VIOLATION);
            return false;
        }
        try {
            var actual = jwt.validar(c.token);
            if (acceso.vigente(actual)) return true;
        } catch (Exception ignored) { /* sin enviar detalles del JWT */ }
        cerrar(c, CloseStatus.POLICY_VIOLATION);
        return false;
    }
    private void cerrar(Conexion c, CloseStatus status) {
        conexiones.remove(c.socket.getId(), c);
        try { if (c.socket.isOpen()) c.socket.close(status); } catch (Exception ignored) { }
    }
    private void enviar(Conexion c, Object evento) {
        try { if (c.socket.isOpen()) c.socket.sendMessage(new TextMessage(json.writeValueAsString(evento))); }
        catch (Exception ignored) { cerrar(c, CloseStatus.SERVER_ERROR); }
    }

    /**
     * Reconciliación de cambios que todavía no emiten eventos (p. ej. Mesero en otra rama).
     * Una sola lectura compartida cada 2 s, solo si hay cocineros conectados; no hay polling HTTP.
     * El socket envía una invalidación únicamente si cambió el contenido del tablero.
     */
    @Scheduled(fixedDelay = 2000)
    public void reconciliar() {
        try {
            var cocineros = conexiones.values().stream().filter(this::autorizada)
                .filter(c -> c.usuario.rol() == Rol.COCINERO).toList();
            if (cocineros.isEmpty()) { huellaAnterior = null; return; }
            String huella = json.writeValueAsString(cocina.instantanea());
            if (!huella.equals(huellaAnterior)) {
                huellaAnterior = huella;
                cocineros.forEach(c -> enviar(c, Map.of("tipo", "TABLERO_CAMBIO")));
            }
        } catch (Exception ex) {
            log.warn("No se pudo reconciliar el tablero de cocina: {}", ex.getClass().getSimpleName());
            conexiones.values().stream().filter(c -> c.usuario != null && c.usuario.rol() == Rol.COCINERO)
                .forEach(c -> enviar(c, Map.of("tipo", "SINCRONIZACION_ERROR")));
        }
    }

    /** Después de confirmar la transacción, no antes. El mesero solo recibe sus pedidos. */
    @Override
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void actualizar(EventoCocina evento) {
        // Se encola tras commit. La entrega ocurre fuera de la transacción ya cerrada.
        if (!eventosPendientes.offer(evento)) log.warn("Cola de notificaciones de Cocina llena; se reconciliará el tablero.");
    }

    @Scheduled(fixedDelay = 250)
    public void entregarEventos() {
        for (int i = 0; i < 100; i++) {
            EventoCocina evento = eventosPendientes.poll();
            if (evento == null) break;
            entregar(evento);
        }
    }

    private void entregar(EventoCocina evento) {
        for (Conexion c : conexiones.values()) {
            try {
                if (!autorizada(c)) continue;
                Rol rol = c.usuario.rol();
                boolean propia = rol == Rol.MESERO && c.meseroId != null && c.meseroId.equals(evento.meseroId());
                boolean disponibilidad = "DISPONIBILIDAD_ACTUALIZADA".equals(evento.tipo());
                if (rol == Rol.COCINERO || propia || (disponibilidad && (rol == Rol.MESERO || rol == Rol.ADMINISTRADOR))) enviar(c, evento);
            } catch (Exception ex) { log.warn("No se pudo enviar una notificación de cocina."); }
        }
    }
}
