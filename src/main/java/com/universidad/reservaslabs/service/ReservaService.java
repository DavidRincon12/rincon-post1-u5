package com.universidad.reservaslabs.service;

import com.universidad.reservaslabs.exception.RecursoNoEncontradoException;
import com.universidad.reservaslabs.exception.ReservaConflictException;
import com.universidad.reservaslabs.exception.ReservaInvalidaException;
import com.universidad.reservaslabs.model.EstadoReserva;
import com.universidad.reservaslabs.model.Laboratorio;
import com.universidad.reservaslabs.model.Reserva;
import com.universidad.reservaslabs.repository.LaboratorioRepository;
import com.universidad.reservaslabs.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReservaService {

    private static final LocalTime APERTURA = LocalTime.of(7, 0);
    private static final LocalTime CIERRE = LocalTime.of(21, 0);
    private static final Duration DURACION_MINIMA = Duration.ofMinutes(30);
    private static final Duration DURACION_MAXIMA = Duration.ofHours(3);

    private final ReservaRepository reservaRepo;
    private final LaboratorioRepository laboratorioRepo;
    private final Clock reloj;

    public ReservaService(ReservaRepository reservaRepo, LaboratorioRepository laboratorioRepo, Clock reloj) {
        this.reservaRepo = reservaRepo;
        this.laboratorioRepo = laboratorioRepo;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<Reserva> findAll() { return reservaRepo.findAll(); }

    @Transactional(readOnly = true)
    public Optional<Reserva> findById(Long id) { return reservaRepo.findById(id); }

    @Transactional(readOnly = true)
    public List<Reserva> findByLaboratorio(Long laboratorioId) {
        if (!laboratorioRepo.existsById(laboratorioId)) {
            throw new RecursoNoEncontradoException("Laboratorio no encontrado: " + laboratorioId);
        }
        return reservaRepo.findByLaboratorioId(laboratorioId);
    }

    public Reserva crear(Reserva reserva) {
        Laboratorio laboratorio = buscarLaboratorio(reserva);
        reserva.setLaboratorio(laboratorio);

        // Regla 1: depende solo de los campos de la propia reserva, por eso
        // se valida aquí con Java puro, sin consultar la base de datos.
        validarHorarioYDuracion(reserva.getInicio(), reserva.getFin());

        // Regla 2: depende de las otras reservas guardadas. El Repository
        // filtra los solapamientos en SQL y el Service decide qué hacer.
        List<Reserva> solapamientos = reservaRepo.buscarSolapamientos(
            laboratorio.getId(), reserva.getInicio(), reserva.getFin());
        if (!solapamientos.isEmpty()) {
            throw new ReservaConflictException(
                "El laboratorio " + laboratorio.getNombre() + " ya tiene una reserva en ese horario");
        }

        reserva.setId(null);
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        return reservaRepo.save(reserva);
    }

    public void cancelar(Long id) {
        Reserva reserva = reservaRepo.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada: " + id));

        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            throw new ReservaConflictException("La reserva " + id + " ya se encuentra cancelada");
        }
        // Regla 3: no se cancela una reserva cuyo horario de inicio ya pasó.
        if (reserva.getInicio().isBefore(LocalDateTime.now(reloj))) {
            throw new ReservaConflictException(
                "No se puede cancelar una reserva cuyo horario de inicio ya pasó");
        }
        reserva.setEstado(EstadoReserva.CANCELADA);
        reservaRepo.save(reserva);
    }

    private Laboratorio buscarLaboratorio(Reserva reserva) {
        if (reserva.getLaboratorio() == null || reserva.getLaboratorio().getId() == null) {
            throw new ReservaInvalidaException("Debe indicar el laboratorio a reservar");
        }
        Long laboratorioId = reserva.getLaboratorio().getId();
        return laboratorioRepo.findById(laboratorioId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Laboratorio no encontrado: " + laboratorioId));
    }

    private void validarHorarioYDuracion(LocalDateTime inicio, LocalDateTime fin) {
        if (inicio == null || fin == null || !fin.isAfter(inicio)) {
            throw new ReservaInvalidaException("El rango de fecha y hora de la reserva es inválido");
        }
        if (!inicio.toLocalDate().equals(fin.toLocalDate())) {
            throw new ReservaInvalidaException("La reserva debe iniciar y terminar el mismo día");
        }
        Duration duracion = Duration.between(inicio, fin);
        if (duracion.compareTo(DURACION_MINIMA) < 0 || duracion.compareTo(DURACION_MAXIMA) > 0) {
            throw new ReservaInvalidaException(
                "La duración de la reserva debe estar entre 30 minutos y 3 horas");
        }
        if (inicio.toLocalTime().isBefore(APERTURA) || fin.toLocalTime().isAfter(CIERRE)) {
            throw new ReservaInvalidaException(
                "La reserva debe estar dentro del horario de atención (07:00 - 21:00)");
        }
    }
}
