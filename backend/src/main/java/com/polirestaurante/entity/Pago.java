package com.polirestaurante.entity;

import com.polirestaurante.enums.EstadoPago;
import com.polirestaurante.enums.MedioPago;
import com.polirestaurante.enums.TipoFactura;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Pago aprobado de una cuenta. Tambien guarda los datos de la factura
 * (no existe tabla Factura en el modelo y no se crean tablas nuevas).
 */
@Entity
@Table(name = "Pago")
@Getter @Setter @NoArgsConstructor
public class Pago {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago") private Integer idPago;
    @Column(name = "monto") private Double monto;
    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago") private MedioPago metodoPago;
    @Column(name = "fecha_pago") private LocalDateTime fechaPago;
    @Column(name = "Pedido_id_pedido", nullable = false) private Integer pedidoId;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false) private EstadoPago estado = EstadoPago.APROBADO;
    @Column(name = "subtotal") private Double subtotal;
    @Column(name = "descuento", nullable = false) private Double descuento = 0.0;
    @Column(name = "impuesto", nullable = false) private Double impuesto = 0.0;
    @Column(name = "promocion") private String promocion;
    @Column(name = "referencia") private String referencia;
    @Column(name = "monto_recibido") private Double montoRecibido;
    @Column(name = "cambio") private Double cambio;
    @Column(name = "numero_factura") private String numeroFactura;
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_factura") private TipoFactura tipoFactura;
    @Column(name = "fecha_factura") private LocalDateTime fechaFactura;
    @Column(name = "codigo_factura_electronica") private String codigoFacturaElectronica;
    @Column(name = "correo_cliente") private String correoCliente;
    @Column(name = "Cajero_id_cajero") private Integer cajeroId;
    @Column(name = "Caja_id_caja") private Integer cajaId;
}
