package com.polirestaurante.controller;

import com.polirestaurante.dto.LoginRequest;
import com.polirestaurante.dto.LoginResponse;
import com.polirestaurante.security.UsuarioSesion;
import com.polirestaurante.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** RF37 / RF38: inicio de sesion por credenciales y renovacion del token. */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return usuarioService.autenticar(req);
    }

    @PostMapping("/renovar")
    public LoginResponse renovar(@AuthenticationPrincipal UsuarioSesion sesion) {
        return usuarioService.renovar(sesion);
    }
}
