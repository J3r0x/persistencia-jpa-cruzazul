package ec.ups.cruzazul.modelo;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Promocion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String nombre;

    private int descuento;          // porcentaje

    private LocalDate inicio;

    private LocalDate fin;

    // N:M  lado inverso de Producto.promociones
    @ManyToMany(mappedBy = "promociones")
    private Set<Producto> productos = new HashSet<>();

    protected Promocion() { }

    public Promocion(String nombre, int descuento, LocalDate inicio, LocalDate fin) {
        this.nombre = nombre;
        this.descuento = descuento;
        this.inicio = inicio;
        this.fin = fin;
    }
}
