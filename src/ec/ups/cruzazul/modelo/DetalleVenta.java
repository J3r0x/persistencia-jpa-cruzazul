package ec.ups.cruzazul.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Entidad asociativa: resuelve la relación N:M entre Venta y Producto y
 * guarda los datos de esa línea (cantidad y precio al momento de la venta).
 */
@Entity
public class DetalleVenta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Venta venta;

    @ManyToOne(optional = false)
    private Producto producto;

    private int cantidad;

    @Column(precision = 8, scale = 2)
    private BigDecimal precioUnitario;   // se copia porque el precio del producto puede cambiar

    protected DetalleVenta() { }

    DetalleVenta(Venta venta, Producto producto, int cantidad, BigDecimal precioUnitario) {
        this.venta = venta;
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }
}
