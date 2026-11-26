package com.polirestaurante.security;

import com.polirestaurante.config.PoliRestauranteProperties;
import com.polirestaurante.enums.Rol;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/** Emite y valida los JSON Web Tokens (ADR-003). */
@Component
public class JwtUtil {

    private final SecretKey clave;
    private final long expiracionMs;

    public JwtUtil(PoliRestauranteProperties props) {
        String secreto = props.getJwt().getSecret();
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("polirestaurante.jwt.secret (variable JWT_SECRET) debe tener al menos 32 caracteres");
        }
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracionMs = props.getJwt().getExpiracionMinutos() * 60_000L;
    }

    public String generar(UsuarioSesion u) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(String.valueOf(u.id()))
                .claim("nombre", u.nombre())
                .claim("rol", u.rol().name())
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expiracionMs))
                .signWith(clave)
                .compact();
    }

    /** Lanza JwtException si el token es invalido o expiro. */
    public UsuarioSesion validar(String token) {
        Claims c = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
        return new UsuarioSesion(Integer.valueOf(c.getSubject()), c.get("nombre", String.class),
                Rol.valueOf(c.get("rol", String.class)));
    }

    public long getExpiracionSegundos() {
        return expiracionMs / 1000;
    }
}
