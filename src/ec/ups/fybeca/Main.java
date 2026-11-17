package ec.ups.fybeca;

import ec.ups.fybeca.modelo.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Modelo académico de la cadena de farmacias Fybeca (Corporación GPF, Ecuador).
 * Crea el esquema en H2, carga datos de prueba y ejecuta tres consultas JPQL
 * con filtros, ordenamiento y paginación.
 */
public class Main {

    /** Fecha fija para que los resultados sean siempre los mismos. */
    static final LocalDate HOY = LocalDate.of(2026, 11, 16);

    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("fybecaPU",
                Map.of("eclipselink.logging.level.sql", System.getProperty("sql.log", "OFF")));
        EntityManager em = emf.createEntityManager();
        cargarDatos(em);
        em.clear(); // las consultas leen desde la base, no desde la caché de la sesión

        consulta1ProductosMasVendidos(em);
        consulta2ClientesVitalcard(em);
        consulta3ProductosEnPromocion(em);

        em.close();
        emf.close();
    }

    /** Productos más vendidos en una ciudad dentro de un rango de fechas, de 5 en 5. */
    static void consulta1ProductosMasVendidos(EntityManager em) {
        System.out.println("\n=== Consulta 1: productos más vendidos en Quito (oct-nov 2026) ===");
        for (int pagina = 1; pagina <= 2; pagina++) {
            List<Object[]> filas = em.createQuery("""
                    SELECT p.nombre, SUM(d.cantidad) AS unidades, SUM(d.cantidad * d.precioUnitario) AS ingreso
                    FROM DetalleVenta d JOIN d.producto p JOIN d.venta v
                    WHERE v.sucursal.ciudad = :ciudad AND v.fecha BETWEEN :desde AND :hasta
                    GROUP BY p.id, p.nombre
                    ORDER BY unidades DESC, p.nombre ASC""", Object[].class)
                    .setParameter("ciudad", "Quito")
                    .setParameter("desde", LocalDate.of(2026, 10, 1))
                    .setParameter("hasta", LocalDate.of(2026, 11, 15))
                    .setFirstResult((pagina - 1) * 5)
                    .setMaxResults(5)
                    .getResultList();
            System.out.println("-- pagina " + pagina);
            for (Object[] f : filas) {
                System.out.printf("%-34s %4s u.  $%s%n", f[0], f[1], f[2]);
            }
        }
    }

    /** Clientes con tarjeta Vitalcard activa y más de cierto puntaje, con su número de compras. */
    static void consulta2ClientesVitalcard(EntityManager em) {
        System.out.println("\n=== Consulta 2: clientes Vitalcard activos con 300 puntos o más (página 1 de 5) ===");
        List<ClienteVitalcard> filas = em.createQuery("""
                SELECT NEW ec.ups.fybeca.modelo.ClienteVitalcard(c.apellidos, c.nombres, t.puntos, COUNT(v))
                FROM Cliente c JOIN c.tarjeta t LEFT JOIN c.ventas v
                WHERE t.activa = TRUE AND t.puntos >= :minimo
                GROUP BY c.id, c.apellidos, c.nombres, t.puntos
                ORDER BY t.puntos DESC, c.apellidos ASC""", ClienteVitalcard.class)
                .setParameter("minimo", 300)
                .setFirstResult(0)
                .setMaxResults(5)
                .getResultList();
        for (ClienteVitalcard c : filas) {
            System.out.printf("%-12s %-10s %5d puntos, %d compras%n", c.apellidos(), c.nombres(), c.puntos(), c.compras());
        }
    }

    /** Productos de venta libre en una promoción vigente, filtrados por nombre. */
    static void consulta3ProductosEnPromocion(EntityManager em) {
        System.out.println("\n=== Consulta 3: productos de venta libre en promoción vigente (página 1 de 10) ===");
        List<Object[]> filas = em.createQuery("""
                SELECT p.nombre, p.precio, pr.nombre, pr.descuento
                FROM Producto p JOIN p.promociones pr
                WHERE :hoy BETWEEN pr.inicio AND pr.fin
                  AND p.requiereReceta = FALSE
                  AND LOWER(p.nombre) LIKE :patron
                ORDER BY pr.descuento DESC, p.nombre ASC""", Object[].class)
                .setParameter("hoy", HOY)
                .setParameter("patron", "%")
                .setFirstResult(0)
                .setMaxResults(10)
                .getResultList();
        for (Object[] f : filas) {
            System.out.printf("%-34s $%-6s %-24s %s%%%n", f[0], f[1], f[2], f[3]);
        }
    }

    private static void cargarDatos(EntityManager em) {
        em.getTransaction().begin();

        // Sucursales: nombres de ejemplo por sector; no son datos internos de la empresa.
        List<Sucursal> sucursales = List.of(
                new Sucursal("UIO-01", "Fybeca El Bosque", "Quito"),
                new Sucursal("UIO-02", "Fybeca Cumbayá", "Quito"),
                new Sucursal("UIO-03", "Fybeca Quicentro Sur", "Quito"),
                new Sucursal("GYE-01", "Fybeca Urdesa", "Guayaquil"),
                new Sucursal("CUE-01", "Fybeca Remigio Crespo", "Cuenca"));
        sucursales.forEach(em::persist);

        Promocion dolor = new Promocion("Semana del alivio", 20, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 23));
        Promocion piel = new Promocion("Cuida tu piel", 30, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
        Promocion vitaminas = new Promocion("Defensas al 15", 15, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 15));
        Promocion pasada = new Promocion("Regreso a clases", 25, LocalDate.of(2026, 8, 15), LocalDate.of(2026, 9, 30));
        List.of(dolor, piel, vitaminas, pasada).forEach(em::persist);

        Object[][] catalogo = {
            {"7861001000011", "Paracetamol 500 mg x 20", "2.10", false},
            {"7861001000028", "Ibuprofeno 400 mg x 10", "3.45", false},
            {"7861001000035", "Amoxicilina 500 mg x 21", "8.90", true},
            {"7861001000042", "Vitamina C 1 g efervescente x 10", "6.75", false},
            {"7861001000059", "Protector solar FPS 50 120 ml", "18.40", false},
            {"7861001000066", "Crema hidratante facial 50 ml", "12.30", false},
            {"7861001000073", "Losartán 50 mg x 30", "11.20", true},
            {"7861001000080", "Alcohol antiséptico 500 ml", "2.60", false},
            {"7861001000097", "Suero oral 500 ml", "1.85", false},
            {"7861001000103", "Multivitamínico adulto x 30", "14.90", false},
            {"7861001000110", "Pañales etapa 3 x 40", "16.50", false},
            {"7861001000127", "Omeprazol 20 mg x 14", "5.40", true},
        };
        List<Producto> productos = new ArrayList<>();
        for (Object[] p : catalogo) {
            Producto prod = new Producto((String) p[0], (String) p[1], new BigDecimal((String) p[2]), (Boolean) p[3]);
            em.persist(prod);
            productos.add(prod);
        }
        productos.get(0).agregarPromocion(dolor);
        productos.get(1).agregarPromocion(dolor);
        productos.get(4).agregarPromocion(piel);
        productos.get(5).agregarPromocion(piel);
        productos.get(3).agregarPromocion(vitaminas);
        productos.get(9).agregarPromocion(vitaminas);
        productos.get(2).agregarPromocion(dolor);        // con receta: la consulta 3 debe excluirlo
        productos.get(10).agregarPromocion(pasada);      // promoción vencida: también se excluye

        String[][] personas = {
            {"Andrade", "María"}, {"Benítez", "Carlos"}, {"Cevallos", "Lucía"}, {"Durán", "Diego"},
            {"Espinoza", "Valeria"}, {"Flores", "Andrés"}, {"Guerrero", "Gabriela"}, {"Herrera", "Jorge"},
            {"Iturralde", "Paola"}, {"Jaramillo", "Sebastián"}, {"López", "Camila"}, {"Molina", "Mateo"},
        };
        List<Cliente> clientes = new ArrayList<>();
        List<TarjetaVitalcard> tarjetas = new ArrayList<>();
        for (int i = 0; i < personas.length; i++) {
            Cliente c = new Cliente(String.format("17%08d", 2000 + i), personas[i][1], personas[i][0],
                    (personas[i][1].charAt(0) + personas[i][0]).toLowerCase() + "@correo.ec");
            if (i % 4 != 3) {   // uno de cada cuatro clientes no tiene Vitalcard
                TarjetaVitalcard t = new TarjetaVitalcard(String.format("6033%012d", 500 + i), 0,
                        i != 6, LocalDate.of(2024, 1 + i, 10), c);
                c.setTarjeta(t);
                tarjetas.add(t);
            } else {
                tarjetas.add(null);
            }
            em.persist(c);  // la tarjeta se guarda en cascada
            clientes.add(c);
        }

        Random azar = new Random(2026);  // semilla fija: siempre los mismos datos
        LocalDate inicio = LocalDate.of(2026, 9, 1);
        for (int n = 0; n < 160; n++) {
            int idx = azar.nextInt(clientes.size() + 3);
            Cliente c = idx < clientes.size() ? clientes.get(idx) : null;   // algunas ventas a consumidor final
            Venta v = new Venta(inicio.plusDays(azar.nextInt(76)), c, sucursales.get(azar.nextInt(sucursales.size())));
            int lineas = 1 + azar.nextInt(3);
            for (int l = 0; l < lineas; l++) {
                v.agregarDetalle(productos.get(azar.nextInt(productos.size())), 1 + azar.nextInt(3));
            }
            em.persist(v);   // los detalles se guardan en cascada
            if (c != null && tarjetas.get(idx) != null) {
                tarjetas.get(idx).sumarPuntos(v.getTotal().intValue() * 2);  // 2 puntos por dólar
            }
        }
        em.getTransaction().commit();
    }
}
