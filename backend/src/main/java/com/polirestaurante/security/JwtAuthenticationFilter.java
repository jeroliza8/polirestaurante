package com.polirestaurante.security;

import com.polirestaurante.dao.UsuarioDAO;
import com.polirestaurante.enums.EstadoUsuario;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lee el encabezado "Authorization: Bearer ..." y autentica la peticion.
 * Tambien verifica que el usuario siga activo: si el administrador lo desactiva
 * (CU-ADM-03) pierde el acceso de inmediato, aunque su token no haya vencido.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UsuarioDAO usuarioDAO;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UsuarioDAO usuarioDAO) {
        this.jwtUtil = jwtUtil;
        this.usuarioDAO = usuarioDAO;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                UsuarioSesion sesion = jwtUtil.validar(header.substring(7));
                boolean activo = usuarioDAO.findById(sesion.id())
                        .map(u -> u.getEstado() == EstadoUsuario.activo)
                        .orElse(false);
                if (activo) {
                    var auth = new UsernamePasswordAuthenticationToken(sesion, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + sesion.rol().name())));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (JwtException | IllegalArgumentException e) {
                // Token invalido o vencido: la peticion sigue sin autenticar y responde 401.
            }
        }
        chain.doFilter(request, response);
    }
}
