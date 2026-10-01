package com.universidad.reservaslabs.exception;

/**
 * La reserva es inválida por sí misma, sin importar qué otras reservas
 * existan: rango de tiempo incoherente, fuera del horario de atención o con
 * una duración no permitida.
 */
public class ReservaInvalidaException extends RuntimeException {
    public ReservaInvalidaException(String mensaje) { super(mensaje); }
}
