package com.polirestaurante.service.state;

import com.polirestaurante.exception.ConflictoException;
import com.polirestaurante.exception.ReglaNegocioException;

/** RF10 / HU-COC-03. No modifica cantidades ni elimina productos. */
public final class DisponibilidadCocina {
    private DisponibilidadCocina() {}

    public static void validar(boolean actual, boolean nueva, Integer cantidad) {
        if (actual == nueva) throw new ConflictoException(nueva
            ? "El producto ya está disponible." : "El producto ya está marcado como agotado.");
        if (nueva && (cantidad == null || cantidad <= 0))
            throw new ReglaNegocioException("No hay existencias. Administración debe registrar el ingreso de mercancía antes de reactivar el producto.");
    }

    public static boolean efectiva(boolean disponible, Integer cantidad) {
        return disponible && cantidad != null && cantidad > 0;
    }
}
