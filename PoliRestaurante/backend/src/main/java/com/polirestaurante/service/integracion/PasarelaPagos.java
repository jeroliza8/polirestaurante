package com.polirestaurante.service.integracion;

/**
 * Interfaz con la pasarela de pagos (actor externo "PasarelaPagos" del diagrama de
 * secuencia CU-CAJ-001, RNF10). Para conectar una pasarela real se implementa esta
 * interfaz y se marca @Primary; el resto del sistema no cambia.
 */
public interface PasarelaPagos {
    RespuestaPasarela autorizarTarjeta(Integer pedidoId, double monto, String ultimos4);
    RespuestaPasarela verificarTransferencia(Integer pedidoId, double monto, String referencia);
}
