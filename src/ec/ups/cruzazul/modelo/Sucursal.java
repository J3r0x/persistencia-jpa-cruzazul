package ec.ups.cruzazul.modelo;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/** Farmacia física de la cadena. */
@Entity
public class Sucursal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String codigo;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 40)
    private String ciudad;

    // 1:N  una sucursal registra muchas ventas
    @OneToMany(mappedBy = "sucursal")
    private List<Venta> ventas = new ArrayList<>();

    protected Sucursal() { }

    public Sucursal(String codigo, String nombre, String ciudad) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.ciudad = ciudad;
    }

    public String getNombre() { return nombre; }
    public String getCiudad() { return ciudad; }
}
