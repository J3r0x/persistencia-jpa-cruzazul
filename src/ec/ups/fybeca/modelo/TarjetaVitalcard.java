package ec.ups.fybeca.modelo;

import jakarta.persistence.*;
import java.time.LocalDate;

/** Tarjeta del programa de beneficios: acumula puntos por compra. */
@Entity
public class TarjetaVitalcard {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 16)
    private String numero;

    private int puntos;

    private boolean activa;

    private LocalDate fechaEmision;

    // 1:1  lado dueño: aquí vive la columna cliente_id (única)
    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    private Cliente cliente;

    protected TarjetaVitalcard() { }

    public TarjetaVitalcard(String numero, int puntos, boolean activa, LocalDate fechaEmision, Cliente cliente) {
        this.numero = numero;
        this.puntos = puntos;
        this.activa = activa;
        this.fechaEmision = fechaEmision;
        this.cliente = cliente;
    }

    public void sumarPuntos(int p) { puntos += p; }
    public int getPuntos() { return puntos; }
}
