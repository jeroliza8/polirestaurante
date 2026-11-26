package com.polirestaurante.service.pago;

import com.polirestaurante.service.integracion.PasarelaPagosSimulada;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** HU-CAJ-001: aprobacion, rechazo y calculo de cambio por medio de pago (Strategy). */
class EstrategiasPagoTest {

    private final EfectivoStrategy efectivo = new EfectivoStrategy();
    private final TarjetaStrategy tarjeta = new TarjetaStrategy(new PasarelaPagosSimulada());
    private final TransferenciaStrategy transferencia = new TransferenciaStrategy(new PasarelaPagosSimulada());

    @Test
    void efectivoCalculaElCambio() {
        ResultadoPago r = efectivo.procesar(new SolicitudCobro(1, 45000, 50000.0, null));
        assertTrue(r.aprobado());
        assertEquals(5000, r.cambio());
    }

    @Test
    void efectivoInsuficienteSeRechaza() {
        assertFalse(efectivo.procesar(new SolicitudCobro(1, 45000, 40000.0, null)).aprobado());
        assertFalse(efectivo.procesar(new SolicitudCobro(1, 45000, null, null)).aprobado());
    }

    @Test
    void tarjetaExigeCuatroDigitosYPuedeSerRechazada() {
        assertFalse(tarjeta.procesar(new SolicitudCobro(1, 45000, null, "12")).aprobado());
        assertFalse(tarjeta.procesar(new SolicitudCobro(1, 45000, null, "0000")).aprobado());
        ResultadoPago ok = tarjeta.procesar(new SolicitudCobro(1, 45000, null, "4242"));
        assertTrue(ok.aprobado());
        assertTrue(ok.referencia().startsWith("**** 4242"));
    }

    @Test
    void transferenciaExigeReferencia() {
        assertFalse(transferencia.procesar(new SolicitudCobro(1, 45000, null, "")).aprobado());
        assertFalse(transferencia.procesar(new SolicitudCobro(1, 45000, null, "RECH123")).aprobado());
        assertTrue(transferencia.procesar(new SolicitudCobro(1, 45000, null, "trx998877")).aprobado());
    }
}
