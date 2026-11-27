package com.polirestaurante.service.integracion;

/** Actor externo "ProveedorFacturaElectronica" del diagrama CU-CAJ-001 (RF19, RNF10). */
public interface ProveedorFacturaElectronica {
    /** Devuelve el codigo unico de la factura electronica (CUFE). Lanza IntegracionException si falla. */
    String emitir(String numeroFactura, double total, String correoCliente);
}
