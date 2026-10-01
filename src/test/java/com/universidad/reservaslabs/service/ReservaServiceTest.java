package com.universidad.reservaslabs.service;

import com.universidad.reservaslabs.exception.RecursoNoEncontradoException;
import com.universidad.reservaslabs.exception.ReservaConflictException;
import com.universidad.reservaslabs.exception.ReservaInvalidaException;
import com.universidad.reservaslabs.model.EstadoReserva;
import com.universidad.reservaslabs.model.Laboratorio;
import com.universidad.reservaslabs.model.Reserva;
import com.universidad.reservaslabs.repository.LaboratorioRepository;
import com.universidad.reservaslabs.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 8, 1, 8, 0);

    @Mock
    private ReservaRepository reservaRepo;

    @Mock
    private LaboratorioRepository laboratorioRepo;

    private ReservaService service;
    private Laboratorio lab;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(ZonedDateTime.of(AHORA, ZONA).toInstant(), ZONA);
        service = new ReservaService(reservaRepo, laboratorioRepo, reloj);
        lab = new Laboratorio(1L, "Lab. Cómputo 3", "Bloque B, piso 2", 30, "COMPUTO");
    }

    private Reserva reserva(LocalDateTime inicio, LocalDateTime fin) {
        Reserva r = new Reserva();
        r.setLaboratorio(new Laboratorio(1L, null, null, null, null));
        r.setNombreSolicitante("Ana Torres");
        r.setCorreoSolicitante("ana@udes.edu.co");
        r.setInicio(inicio);
        r.setFin(fin);
        return r;
    }

    @Test
    void crearReservaLibreQuedaConfirmada() {
        when(laboratorioRepo.findById(1L)).thenReturn(Optional.of(lab));
        when(reservaRepo.buscarSolapamientos(any(), any(), any())).thenReturn(List.of());
        when(reservaRepo.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        Reserva creada = service.crear(reserva(
            LocalDateTime.of(2026, 8, 10, 9, 0), LocalDateTime.of(2026, 8, 10, 11, 0)));

        assertThat(creada.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(creada.getLaboratorio().getNombre()).isEqualTo("Lab. Cómputo 3");
    }

    @Test
    void crearReservaSolapadaLanzaConflicto() {
        when(laboratorioRepo.findById(1L)).thenReturn(Optional.of(lab));
        when(reservaRepo.buscarSolapamientos(any(), any(), any())).thenReturn(List.of(new Reserva()));

        assertThatThrownBy(() -> service.crear(reserva(
            LocalDateTime.of(2026, 8, 10, 10, 0), LocalDateTime.of(2026, 8, 10, 12, 0))))
            .isInstanceOf(ReservaConflictException.class)
            .hasMessageContaining("ya tiene una reserva en ese horario");
        verify(reservaRepo, never()).save(any());
    }

    @Test
    void crearReservaFueraDeHorarioNoConsultaSolapamientos() {
        when(laboratorioRepo.findById(1L)).thenReturn(Optional.of(lab));

        assertThatThrownBy(() -> service.crear(reserva(
            LocalDateTime.of(2026, 8, 10, 22, 0), LocalDateTime.of(2026, 8, 10, 23, 0))))
            .isInstanceOf(ReservaInvalidaException.class)
            .hasMessageContaining("horario de atención");
        verify(reservaRepo, never()).buscarSolapamientos(any(), any(), any());
    }

    @Test
    void crearReservaConDuracionInvalidaLanzaExcepcion() {
        when(laboratorioRepo.findById(1L)).thenReturn(Optional.of(lab));

        assertThatThrownBy(() -> service.crear(reserva(
            LocalDateTime.of(2026, 8, 10, 9, 0), LocalDateTime.of(2026, 8, 10, 9, 15))))
            .isInstanceOf(ReservaInvalidaException.class)
            .hasMessageContaining("30 minutos y 3 horas");
    }

    @Test
    void crearReservaConFinAntesDelInicioLanzaExcepcion() {
        when(laboratorioRepo.findById(1L)).thenReturn(Optional.of(lab));

        assertThatThrownBy(() -> service.crear(reserva(
            LocalDateTime.of(2026, 8, 10, 11, 0), LocalDateTime.of(2026, 8, 10, 9, 0))))
            .isInstanceOf(ReservaInvalidaException.class);
    }

    @Test
    void crearReservaEnLaboratorioInexistenteLanzaNoEncontrado() {
        when(laboratorioRepo.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(reserva(
            LocalDateTime.of(2026, 8, 10, 9, 0), LocalDateTime.of(2026, 8, 10, 11, 0))))
            .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void cancelarReservaFuturaLaMarcaComoCancelada() {
        Reserva existente = reserva(LocalDateTime.of(2026, 8, 10, 9, 0), LocalDateTime.of(2026, 8, 10, 11, 0));
        existente.setEstado(EstadoReserva.CONFIRMADA);
        when(reservaRepo.findById(5L)).thenReturn(Optional.of(existente));

        service.cancelar(5L);

        assertThat(existente.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
        verify(reservaRepo).save(existente);
    }

    @Test
    void cancelarReservaQueYaInicioLanzaConflicto() {
        Reserva pasada = reserva(AHORA.minusHours(1), AHORA.plusHours(1));
        pasada.setEstado(EstadoReserva.CONFIRMADA);
        when(reservaRepo.findById(5L)).thenReturn(Optional.of(pasada));

        assertThatThrownBy(() -> service.cancelar(5L))
            .isInstanceOf(ReservaConflictException.class)
            .hasMessageContaining("ya pasó");
        verify(reservaRepo, never()).save(any());
    }

    @Test
    void cancelarReservaInexistenteLanzaNoEncontrado() {
        when(reservaRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelar(99L))
            .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
