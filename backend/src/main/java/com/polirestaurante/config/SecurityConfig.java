package com.polirestaurante.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.polirestaurante.dao.UsuarioDAO;
import com.polirestaurante.security.JwtAuthenticationFilter;
import com.polirestaurante.security.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.Map;

/**
 * ADR-003: control de acceso basado en roles (RBAC) con JWT, sin sesion en servidor.
 * Cada rol solo entra a su prefijo de API (RF38, RNF04).
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final UsuarioDAO usuarioDAO;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())                       // API sin cookies: el token va en el encabezado
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/*.html", "/css/**", "/js/**", "/img/**", "/favicon.ico", "/error").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
            // El socket autentica el primer frame antes de enviar datos; las APIs de Cocina son privadas.
            .requestMatchers("/ws/cocina").permitAll()
            .requestMatchers("/api/cocinero/**").hasRole("COCINERO")
            .requestMatchers("/api/admin/**").hasRole("ADMINISTRADOR")
            .requestMatchers("/api/cajero/**").hasRole("CAJERO")
            .requestMatchers("/api/proveedor/**").hasRole("PROVEEDOR")
            .anyRequest().authenticated())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) ->
                    escribirError(res, HttpServletResponse.SC_UNAUTHORIZED, "Debe iniciar sesión o su sesión expiró"))
                .accessDeniedHandler((req, res, ex) ->
                    escribirError(res, HttpServletResponse.SC_FORBIDDEN, "Su rol no tiene permiso para esta acción")))
            // El filtro NO se registra como @Component para que no se ejecute dos veces (servlet + seguridad)
            .addFilterBefore(new JwtAuthenticationFilter(jwtUtil, usuarioDAO), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** RNF05: contrasenas con BCrypt. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private void escribirError(HttpServletResponse res, int status, String mensaje) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(res.getWriter(), Map.of("error", mensaje));
    }
}
