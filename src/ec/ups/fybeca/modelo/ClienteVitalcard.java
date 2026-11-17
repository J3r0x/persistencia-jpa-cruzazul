package ec.ups.fybeca.modelo;

/** DTO que llena la consulta 2 con una expresión SELECT NEW. */
public record ClienteVitalcard(String apellidos, String nombres, int puntos, long compras) { }
