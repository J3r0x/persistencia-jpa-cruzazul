package ec.ups.cruzazul.concurrencia;

import ec.ups.cruzazul.modelo.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.Persistence;
import jakarta.persistence.RollbackException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Unidad 4: control de concurrencia y aislamiento de transacciones sobre el
 * modelo de Farmacias Cruz Azul. Simula dos cajeros que trabajan a la vez.
 */
public class DemoConcurrencia {

    static final String URL = "jdbc:h2:mem:demo;DB_CLOSE_DELAY=-1;MODE=LEGACY;LOCK_TIMEOUT=10000";

    static long idCuenta;
    static long idStock;

    public static void main(String[] args) throws Exception {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("cruzazulPU",
                Map.of("jakarta.persistence.jdbc.url", URL));
        preparar(emf);
        escenario1ActualizacionPerdida(emf);
        escenario2BloqueoOptimista(emf);
        escenario3BloqueoPesimista(emf);
        escenario4NivelesDeAislamiento();
        emf.close();
    }

    static void preparar(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Sucursal local = new Sucursal("UIO-01", "Cruz Azul La Carolina", "Quito");
        Producto prod = new Producto("7861001000059", "Protector solar FPS 50 120 ml", new BigDecimal("18.40"), false);
        Cliente cli = new Cliente("1700002000", "María", "Andrade", "mandrade@correo.ec");
        CuentaPuntos cuenta = new CuentaPuntos("PCA-0000000500", 500, true, LocalDate.of(2025, 3, 1), cli);
        cli.setCuentaPuntos(cuenta);
        StockLocal stock = new StockLocal(prod, local, 3);
        em.persist(local);
        em.persist(prod);
        em.persist(cli);
        em.persist(stock);
        em.getTransaction().commit();
        idCuenta = em.createQuery("SELECT t.id FROM CuentaPuntos t", Long.class).getSingleResult();
        idStock = stock.getId();
        em.close();
    }

    /** Sin control: los dos cajeros leen 500 puntos y el segundo commit borra el canje del primero. */
    static void escenario1ActualizacionPerdida(EntityManagerFactory emf) {
        System.out.println("\n=== 1. Sin control de concurrencia (actualización perdida) ===");
        EntityManager cajeroA = emf.createEntityManager();
        EntityManager cajeroB = emf.createEntityManager();
        cajeroA.getTransaction().begin();
        cajeroB.getTransaction().begin();
        CuentaPuntos a = cajeroA.find(CuentaPuntos.class, idCuenta);
        CuentaPuntos b = cajeroB.find(CuentaPuntos.class, idCuenta);
        System.out.println("Cajero A lee " + a.getPuntos() + " puntos; cajero B lee " + b.getPuntos());
        a.sumarPuntos(-200);   // canje de 200 puntos
        b.sumarPuntos(-150);   // canje de 150 puntos en otra caja
        cajeroA.getTransaction().commit();
        cajeroB.getTransaction().commit();
        cajeroA.close();
        cajeroB.close();
        EntityManager em = emf.createEntityManager();
        int finales = em.find(CuentaPuntos.class, idCuenta).getPuntos();
        System.out.println("Saldo final: " + finales + " (debía ser 150; se perdió el canje del cajero A)");
        em.close();
    }

    /** Con @Version: el segundo commit falla y el cajero vuelve a leer antes de decidir. */
    static void escenario2BloqueoOptimista(EntityManagerFactory emf) {
        System.out.println("\n=== 2. Bloqueo optimista con @Version ===");
        EntityManager cajeroA = emf.createEntityManager();
        EntityManager cajeroB = emf.createEntityManager();
        cajeroA.getTransaction().begin();
        cajeroB.getTransaction().begin();
        StockLocal a = cajeroA.find(StockLocal.class, idStock);
        StockLocal b = cajeroB.find(StockLocal.class, idStock);
        System.out.println("Ambos cajeros ven " + a.getCantidad() + " unidades (versión " + a.getVersion() + ")");
        a.vender(2);
        b.vender(2);
        cajeroA.getTransaction().commit();
        System.out.println("Cajero A vende 2: quedan " + a.getCantidad() + " (versión " + a.getVersion() + ")");
        try {
            cajeroB.getTransaction().commit();
            System.out.println("Cajero B también vendió (sobreventa)");
        } catch (RollbackException e) {
            boolean optimista = e.getCause() instanceof OptimisticLockException;
            System.out.println("Cajero B: commit rechazado (" + (optimista ? "OptimisticLockException" : e.getCause()) + ")");
            cajeroB.clear();
            cajeroB.getTransaction().begin();
            StockLocal actual = cajeroB.find(StockLocal.class, idStock);
            try {
                actual.vender(2);
                cajeroB.getTransaction().commit();
            } catch (IllegalStateException ex) {
                cajeroB.getTransaction().rollback();
                System.out.println("Cajero B reintenta, lee " + actual.getCantidad() + " unidad y no vende: " + ex.getMessage());
            }
        }
        cajeroA.close();
        cajeroB.close();
    }

    /** PESSIMISTIC_WRITE: el segundo cajero espera a que el primero termine (SELECT ... FOR UPDATE). */
    static void escenario3BloqueoPesimista(EntityManagerFactory emf) throws Exception {
        System.out.println("\n=== 3. Bloqueo pesimista (PESSIMISTIC_WRITE) ===");
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.find(StockLocal.class, idStock).reponer(4);   // vuelve a 5 unidades
        em.getTransaction().commit();
        em.close();

        CountDownLatch aTieneElBloqueo = new CountDownLatch(1);
        long[] esperaB = new long[1];
        Thread hiloA = new Thread(() -> {
            EntityManager e = emf.createEntityManager();
            e.getTransaction().begin();
            StockLocal s = e.find(StockLocal.class, idStock, LockModeType.PESSIMISTIC_WRITE);
            aTieneElBloqueo.countDown();
            dormir(1500);   // el cajero A tarda en cobrar
            s.vender(3);
            e.getTransaction().commit();
            System.out.println("Cajero A vende 3 y libera el bloqueo");
            e.close();
        });
        Thread hiloB = new Thread(() -> {
            esperar(aTieneElBloqueo);
            EntityManager e = emf.createEntityManager();
            e.getTransaction().begin();
            long ini = System.nanoTime();
            StockLocal s = e.find(StockLocal.class, idStock, LockModeType.PESSIMISTIC_WRITE);
            esperaB[0] = (System.nanoTime() - ini) / 1_000_000;
            System.out.println("Cajero B obtuvo el bloqueo tras esperar " + esperaB[0] + " ms y ve " + s.getCantidad() + " unidades");
            try {
                s.vender(3);
                e.getTransaction().commit();
            } catch (IllegalStateException ex) {
                e.getTransaction().rollback();
                System.out.println("Cajero B no vende: " + ex.getMessage());
            }
            e.close();
        });
        hiloA.start();
        hiloB.start();
        hiloA.join();
        hiloB.join();
    }

    /** Lectura no repetible con READ COMMITTED y lectura estable con REPEATABLE READ (JDBC sobre H2). */
    static void escenario4NivelesDeAislamiento() throws Exception {
        System.out.println("\n=== 4. Niveles de aislamiento ===");
        for (int nivel : new int[] {Connection.TRANSACTION_READ_COMMITTED, Connection.TRANSACTION_REPEATABLE_READ}) {
            try (Connection reporte = DriverManager.getConnection(URL, "sa", "");
                 Connection caja = DriverManager.getConnection(URL, "sa", "")) {
                reporte.setAutoCommit(false);
                reporte.setTransactionIsolation(nivel);
                int primera = leerStock(reporte);
                try (Statement st = caja.createStatement()) {   // otra caja vende 1 unidad y confirma
                    st.executeUpdate("UPDATE STOCKLOCAL SET CANTIDAD = CANTIDAD - 1, VERSION = VERSION + 1 WHERE ID = " + idStock);
                }
                int segunda = leerStock(reporte);
                reporte.commit();
                String nombre = nivel == Connection.TRANSACTION_READ_COMMITTED ? "READ COMMITTED" : "REPEATABLE READ";
                System.out.printf("%-16s primera lectura %d, segunda lectura %d -> %s%n", nombre, primera, segunda,
                        primera == segunda ? "lectura estable" : "lectura no repetible");
            }
        }
    }

    static int leerStock(Connection c) throws Exception {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT CANTIDAD FROM STOCKLOCAL WHERE ID = " + idStock)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static void esperar(CountDownLatch l) {
        try {
            l.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
