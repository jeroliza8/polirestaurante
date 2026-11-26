package com.polirestaurante.service.pago;

import com.polirestaurante.enums.MedioPago;
import com.polirestaurante.service.integracion.IntegracionException;
import com.polirestaurante.service.integracion.PasarelaPagos;
import com.polirestaurante.service.integracion.RespuestaPasarela;
import org.springframework.stereotype.Component;

/**
 * Tarjeta: se autoriza en la pasarela de pagos externa (RNF10) y puede ser
 * rechazada (HU-CAJ-001 AC4). Solo se guardan los ultimos 4 digitos.
 */
@Component
public class TarjetaStrategy implements MedioPagoStrategy {

    private final PasarelaPagos pasarela;

    public TarjetaStrategy(PasarelaPagos pasarela) {
        this.pasarela = pasarela;
    }

    @Override
    public MedioPago medio() {
        return MedioPago.TARJETA;
    }

    @Override
    public ResultadoPago procesar(SolicitudCobro s) {
        String ultimos4 = s.referencia() == null ? "" : s.referencia().trim();
        if (!ultimos4.matches("\\d{4}")) {
            return ResultadoPago.rechazado("ingrese los últimos 4 dígitos de la tarjeta");
        }
        try {
            RespuestaPasarela r = pasarela.autorizarTarjeta(s.pedidoId(), s.total(), ultimos4);
            if (!r.aprobado()) {
                return ResultadoPago.rechazado(r.mensaje());
            }
            return ResultadoPago.aprobado(null, null, "**** " + ultimos4 + " / " + r.codigoAutorizacion());
        } catch (IntegracionException e) {
            return ResultadoPago.rechazado("la pasarela de pagos no respondió, reintente");
        }
    }
}
