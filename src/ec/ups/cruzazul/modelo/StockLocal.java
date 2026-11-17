package ec.ups.cruzazul.modelo;

import jakarta.persistence.*;

/**
 * Existencias de un producto en un local (Unidad 4).
 * El campo {@code @Version} activa el bloqueo optimista: cada UPDATE incluye
 * "WHERE VERSION = ?" y, si otra transacción ya cambió la fila, JPA lanza
 * OptimisticLockException en lugar de sobrescribir su cambio.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"PRODUCTO_ID", "SUCURSAL_ID"}))
public class StockLocal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Producto producto;

    @ManyToOne(optional = false)
    private Sucursal sucursal;

    private int cantidad;

    @Version
    private int version;

    protected StockLocal() { }

    public StockLocal(Producto producto, Sucursal sucursal, int cantidad) {
        this.producto = producto;
        this.sucursal = sucursal;
        this.cantidad = cantidad;
    }

    /** Descuenta unidades; si no alcanzan, rechaza la venta. */
    public void vender(int unidades) {
        if (unidades > cantidad) {
            throw new IllegalStateException("Stock insuficiente: quedan " + cantidad);
        }
        cantidad -= unidades;
    }

    public void reponer(int unidades) { cantidad += unidades; }
    public Long getId() { return id; }
    public int getCantidad() { return cantidad; }
    public int getVersion() { return version; }
}
