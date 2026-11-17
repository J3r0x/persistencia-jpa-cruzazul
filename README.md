# Modelo ORM con JPA: cadena de farmacias Fybeca

Tarea de la Unidad 3 de Persistencia de Datos (Universidad Politécnica Salesiana).
Autor: Isaac Jeremy Rivera Morán.

Modelo académico de una cadena de farmacias basado en información pública de Fybeca
(Corporación GPF, Ecuador). Los datos que carga el programa son de prueba.

## Entidades y relaciones

| Relación | Entidades | Mapeo |
|---|---|---|
| 1:1 | Cliente, TarjetaVitalcard | `@OneToOne` (la tarjeta es la dueña, FK única) |
| 1:N | Sucursal, Venta | `@OneToMany(mappedBy)` / `@ManyToOne` |
| 1:N | Cliente, Venta | `@OneToMany(mappedBy)` / `@ManyToOne` |
| 1:N | Venta, DetalleVenta | composición con `cascade = ALL` y `orphanRemoval` |
| N:M | Producto, Promocion | `@ManyToMany` con `@JoinTable(producto_promocion)` |
| N:M con datos | Venta, Producto | resuelta con la entidad `DetalleVenta` (cantidad, precio) |

## Consultas JPQL (en `Main.java`)

1. Productos más vendidos en una ciudad entre dos fechas (`SUM`, `GROUP BY`, orden por unidades, páginas de 5).
2. Clientes con Vitalcard activa y puntaje mínimo, con número de compras (`SELECT NEW` a un DTO, `LEFT JOIN`).
3. Productos de venta libre en promoción vigente (`JOIN` sobre la colección N:M, `BETWEEN`, `LIKE`).

## Cómo ejecutar

Requiere JDK 17 o superior. Las librerías (EclipseLink 4.0.4, Jakarta Persistence 3.1, H2 2.2) están en `lib/`.

```bash
javac -encoding UTF-8 -cp "lib/*" -d out $(find src -name "*.java")
cp -r src/META-INF out/
java -cp "out:lib/*" ec.ups.fybeca.Main          # en Windows usar "out;lib/*"
java -Dsql.log=FINE -cp "out:lib/*" ec.ups.fybeca.Main   # muestra el SQL generado
```

Con Maven: `mvn compile exec:java`
