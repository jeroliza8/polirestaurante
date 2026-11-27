package com.polirestaurante.service.pago;

import com.polirestaurante.enums.MedioPago;

/**
 * Patron Strategy (seccion 15.3, modulo Cajero): cada medio de pago tiene su propia
 * logica. CajeroService es el contexto y elige la estrategia segun el medio.
 * Agregar un medio nuevo = agregar una clase, sin editar CajeroService.
 */
public interface MedioPagoStrategy {
    MedioPago medio();
    ResultadoPago procesar(SolicitudCobro solicitud);
}
