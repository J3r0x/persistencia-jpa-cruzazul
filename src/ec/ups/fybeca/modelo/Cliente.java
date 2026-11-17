package ec.ups.fybeca.modelo;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Cliente {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String cedula;

    @Column(nullable = false, length = 60)
    private String nombres;

    @Column(nullable = false, length = 60)
    private String apellidos;

    @Column(length = 80)
    private String email;

    // 1:1  cada cliente puede tener una sola tarjeta Vitalcard; la FK está en la tarjeta
    @OneToOne(mappedBy = "cliente", cascade = CascadeType.ALL)
    private TarjetaVitalcard tarjeta;

    // 1:N  un cliente hace muchas compras
    @OneToMany(mappedBy = "cliente")
    private List<Venta> ventas = new ArrayList<>();

    protected Cliente() { }

    public Cliente(String cedula, String nombres, String apellidos, String email) {
        this.cedula = cedula;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.email = email;
    }

    public void setTarjeta(TarjetaVitalcard tarjeta) { this.tarjeta = tarjeta; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
}
