package ec.ups.cruzazul.modelo;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Afiliación del cliente al Plan de Puntos: acumula puntos por cada dólar
 * de compra, que luego se pueden usar para pagar compras futuras.
 */
@Entity
public class CuentaPuntos {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 16)
    private String codigo;

    private int puntos;

    private boolean activa;

    private LocalDate fechaAfiliacion;

    // 1:1  lado dueño: aquí vive la columna cliente_id (única)
    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    private Cliente cliente;

    protected CuentaPuntos() { }

    public CuentaPuntos(String codigo, int puntos, boolean activa, LocalDate fechaAfiliacion, Cliente cliente) {
        this.codigo = codigo;
        this.puntos = puntos;
        this.activa = activa;
        this.fechaAfiliacion = fechaAfiliacion;
        this.cliente = cliente;
    }

    public void sumarPuntos(int p) { puntos += p; }
    public int getPuntos() { return puntos; }
}
