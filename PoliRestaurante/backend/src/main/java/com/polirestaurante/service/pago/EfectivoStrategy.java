package com.polirestaurante.service.pago;

import com.polirestaurante.enums.MedioPago;
import com.polirestaurante.service.CalculadoraCuenta;
import org.springframework.stereotype.Component;

/** Efectivo: valida el monto recibido y calcula el cambio. */
@Component
public class EfectivoStrategy implements MedioPagoStrategy {

    @Override
    public MedioPago medio() {
        return MedioPago.EFECTIVO;
    }

    @Override
    public ResultadoPago procesar(SolicitudCobro s) {
        if (s.montoRecibido() == null) {
            return ResultadoPago.rechazado("indique el monto recibido en efectivo");
        }
        if (s.montoRecibido() < s.total()) {
            return ResultadoPago.rechazado("el monto recibido es menor al total de la cuenta");
        }
        double cambio = CalculadoraCuenta.redondear(s.montoRecibido() - s.total());
        return ResultadoPago.aprobado(s.montoRecibido(), cambio, null);
    }
}
