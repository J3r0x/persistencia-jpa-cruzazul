# Modelo ORM con JPA: Farmacias Cruz Azul

Tarea de la Unidad 3 de Persistencia de Datos (Universidad Politécnica Salesiana).
Autor: Isaac Jeremy Rivera Morán.

Modelo académico de la cadena Farmacias Cruz Azul (Grupo DIFARE, Ecuador), basado en información
pública de la empresa (https://www.farmaciascruzazul.ec). Los datos que carga el programa son de prueba.

## Entidades y relaciones

| Relación | Entidades | Mapeo |
|---|---|---|
| 1:1 | Cliente, CuentaPuntos (Plan de Puntos) | `@OneToOne` (la cuenta es la dueña, FK única) |
| 1:N | Sucursal, Venta | `@OneToMany(mappedBy)` / `@ManyToOne` |
| 1:N | Cliente, Venta | `@OneToMany(mappedBy)` / `@ManyToOne` |
| 1:N | Venta, DetalleVenta | composición con `cascade = ALL` y `orphanRemoval` |
| N:M | Producto, Promocion | `@ManyToMany` con `@JoinTable(producto_promocion)` |
| N:M con datos | Venta, Producto | resuelta con la entidad `DetalleVenta` (cantidad, precio) |

## Consultas JPQL (en `Main.java`)

1. Productos más vendidos en una ciudad entre dos fechas (`SUM`, `GROUP BY`, orden por unidades, páginas de 5).
2. Clientes del Plan de Puntos con cuenta activa y puntaje mínimo, con número de compras (`SELECT NEW` a un DTO, `LEFT JOIN`).
3. Productos de venta libre en promoción vigente (`JOIN` sobre la colección N:M, `BETWEEN`, `LIKE`).

## Cómo ejecutar

Requiere JDK 17 o superior. Las librerías (EclipseLink 4.0.4, Jakarta Persistence 3.1, H2 2.2) están en `lib/`.

```bash
javac -encoding UTF-8 -cp "lib/*" -d out $(find src -name "*.java")
cp -r src/META-INF out/
java -cp "out:lib/*" ec.ups.cruzazul.Main          # en Windows usar "out;lib/*"
java -Dsql.log=FINE -cp "out:lib/*" ec.ups.cruzazul.Main   # muestra el SQL generado
```

Con Maven: `mvn compile exec:java`

## Unidad 4: control de concurrencia (`concurrencia/DemoConcurrencia.java`)

Simula dos cajeros que trabajan a la vez sobre la misma cuenta de puntos o el mismo stock:

1. Sin control: los dos leen 500 puntos y el segundo commit borra el canje del primero (actualización perdida).
2. Bloqueo optimista: la entidad `StockLocal` tiene `@Version`; el segundo commit lanza `OptimisticLockException`.
3. Bloqueo pesimista: `LockModeType.PESSIMISTIC_WRITE` (SELECT ... FOR UPDATE); el segundo cajero espera.
4. Niveles de aislamiento con JDBC: lectura no repetible en READ COMMITTED y lectura estable en REPEATABLE READ.

```bash
java -cp "out:lib/*" ec.ups.cruzazul.concurrencia.DemoConcurrencia
```
