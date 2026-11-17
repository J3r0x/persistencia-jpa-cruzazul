package ec.ups.cruzazul.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Producto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 13)
    private String codigoBarras;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal precio;

    private boolean requiereReceta;

    // N:M  un producto puede estar en varias promociones y una promoción incluye varios productos.
    // JPA crea la tabla intermedia producto_promocion.
    @ManyToMany
    @JoinTable(name = "producto_promocion",
            joinColumns = @JoinColumn(name = "producto_id"),
            inverseJoinColumns = @JoinColumn(name = "promocion_id"))
    private Set<Promocion> promociones = new HashSet<>();

    protected Producto() { }

    public Producto(String codigoBarras, String nombre, BigDecimal precio, boolean requiereReceta) {
        this.codigoBarras = codigoBarras;
        this.nombre = nombre;
        this.precio = precio;
        this.requiereReceta = requiereReceta;
    }

    public void agregarPromocion(Promocion p) { promociones.add(p); }
    public String getNombre() { return nombre; }
    public BigDecimal getPrecio() { return precio; }
}
