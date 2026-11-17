package ec.ups.cruzazul.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Factura de una compra en una sucursal. */
@Entity
public class Venta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(precision = 10, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    // N:1  muchas ventas pertenecen a un cliente (null = consumidor final)
    @ManyToOne
    private Cliente cliente;

    // N:1  muchas ventas se hacen en una sucursal
    @ManyToOne(optional = false)
    private Sucursal sucursal;

    // 1:N  composición: los detalles se guardan y se borran junto con la venta
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleVenta> detalles = new ArrayList<>();

    protected Venta() { }

    public Venta(LocalDate fecha, Cliente cliente, Sucursal sucursal) {
        this.fecha = fecha;
        this.cliente = cliente;
        this.sucursal = sucursal;
    }

    /** Agrega una línea manteniendo sincronizados los dos lados de la relación. */
    public void agregarDetalle(Producto producto, int cantidad) {
        DetalleVenta d = new DetalleVenta(this, producto, cantidad, producto.getPrecio());
        detalles.add(d);
        total = total.add(producto.getPrecio().multiply(BigDecimal.valueOf(cantidad)));
    }

    public BigDecimal getTotal() { return total; }
}
