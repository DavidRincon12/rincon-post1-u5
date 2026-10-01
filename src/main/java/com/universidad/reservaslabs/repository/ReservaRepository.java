package com.universidad.reservaslabs.repository;

import com.universidad.reservaslabs.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByLaboratorioId(Long laboratorioId);

    // Reservas activas (no canceladas) del mismo laboratorio cuyo rango
    // [inicio, fin) se solapa con el rango dado. El filtro se hace en la base
    // de datos y no en memoria para que no dependa del volumen de reservas
    // acumuladas. Esta consulta solo responde una pregunta de datos; la
    // decision de rechazar la reserva la toma ReservaService.
    @Query("""
        SELECT r FROM Reserva r
        WHERE r.laboratorio.id = :laboratorioId
          AND r.estado <> com.universidad.reservaslabs.model.EstadoReserva.CANCELADA
          AND r.inicio < :fin
          AND r.fin > :inicio
        """)
    List<Reserva> buscarSolapamientos(@Param("laboratorioId") Long laboratorioId,
                                      @Param("inicio") LocalDateTime inicio,
                                      @Param("fin") LocalDateTime fin);
}
