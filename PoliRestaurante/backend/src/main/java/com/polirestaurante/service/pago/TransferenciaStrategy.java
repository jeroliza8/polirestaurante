package com.polirestaurante.service.pago;

import com.polirestaurante.enums.MedioPago;
import com.polirestaurante.service.integracion.IntegracionException;
import com.polirestaurante.service.integracion.PasarelaPagos;
import com.polirestaurante.service.integracion.RespuestaPasarela;
import org.springframework.stereotype.Component;

/** Transferencia: exige la referencia del banco y la verifica en la pasarela. */
@Component
public class TransferenciaStrategy implements MedioPagoStrategy {

    private final PasarelaPagos pasarela;

    public TransferenciaStrategy(PasarelaPagos pasarela) {
        this.pasarela = pasarela;
    }

    @Override
    public MedioPago medio() {
        return MedioPago.TRANSFERENCIA;
    }

    @Override
    public ResultadoPago procesar(SolicitudCobro s) {
        String ref = s.referencia() == null ? "" : s.referencia().trim().toUpperCase();
        if (ref.length() < 4) {
            return ResultadoPago.rechazado("ingrese la referencia de la transferencia (mínimo 4 caracteres)");
        }
        try {
            RespuestaPasarela r = pasarela.verificarTransferencia(s.pedidoId(), s.total(), ref);
            return r.aprobado() ? ResultadoPago.aprobado(null, null, ref) : ResultadoPago.rechazado(r.mensaje());
        } catch (IntegracionException e) {
            return ResultadoPago.rechazado("no fue posible verificar la transferencia, reintente");
        }
    }
}
