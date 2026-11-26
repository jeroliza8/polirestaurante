package com.polirestaurante.service.integracion;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Proveedor de facturacion electronica simulado.
 * Para probar la falla externa (RNF10) use un correo que contenga la palabra "falla".
 */
@Component
public class ProveedorFacturaElectronicaSimulado implements ProveedorFacturaElectronica {

    @Override
    public String emitir(String numeroFactura, double total, String correoCliente) {
        if (correoCliente != null && correoCliente.toLowerCase().contains("falla")) {
            throw new IntegracionException("El proveedor de factura electrónica no respondió (falla simulada)");
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest((numeroFactura + "|" + total).getBytes(StandardCharsets.UTF_8));
            return "CUFE-SIM-" + HexFormat.of().formatHex(hash).substring(0, 24).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            throw new IntegracionException("No fue posible generar el CUFE");
        }
    }
}
