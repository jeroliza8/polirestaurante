package com.polirestaurante.service.integracion;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Pasarela simulada para el entorno academico.
 * Para probar el flujo "pago rechazado" (HU-CAJ-001 AC4):
 *   - tarjeta terminada en 0000 -> rechazada por fondos insuficientes
 *   - transferencia cuya referencia empieza por RECH -> rechazada
 */
@Component
public class PasarelaPagosSimulada implements PasarelaPagos {

    @Override
    public RespuestaPasarela autorizarTarjeta(Integer pedidoId, double monto, String ultimos4) {
        if ("0000".equals(ultimos4)) {
            return new RespuestaPasarela(false, null, "fondos insuficientes (rechazo simulado)");
        }
        return new RespuestaPasarela(true, "AUT" + ThreadLocalRandom.current().nextInt(100000, 999999), "Aprobado");
    }

    @Override
    public RespuestaPasarela verificarTransferencia(Integer pedidoId, double monto, String referencia) {
        if (referencia.startsWith("RECH")) {
            return new RespuestaPasarela(false, null, "la transferencia no aparece en el banco (rechazo simulado)");
        }
        return new RespuestaPasarela(true, referencia, "Aprobado");
    }
}
