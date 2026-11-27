package com.polirestaurante.service.integracion;

import org.springframework.stereotype.Component;

import java.util.UUID;

/** Implementacion simulada del proveedor de factura electronica (sin conexion real a la DIAN). */
@Component
public class ProveedorFacturaElectronicaSimulado implements ProveedorFacturaElectronica {

    @Override
    public String emitir(String numeroFactura, double total, String correoCliente) {
        // CUFE simulado: en produccion lo devolveria el proveedor tecnologico
        return "CUFE-SIM-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }
}