package com.universidad.reservaslabs.exception;

/**
 * La reserva choca con el estado actual de los datos: el laboratorio ya está
 * ocupado en ese horario o la reserva ya no se puede cancelar.
 */
public class ReservaConflictException extends RuntimeException {
    public ReservaConflictException(String mensaje) { super(mensaje); }
}
