package com.universidad.reservaslabs.web;

import com.universidad.reservaslabs.model.Laboratorio;
import com.universidad.reservaslabs.repository.LaboratorioRepository;
import com.universidad.reservaslabs.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifica que una misma regla de negocio produce el mismo mensaje en la API
 * REST (JSON) y en la vista MVC (mensaje flash tras la redirección).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReservaWebControllerTest {

    private static final String MENSAJE_CONFLICTO = "El laboratorio Lab. Redes ya tiene una reserva en ese horario";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private LaboratorioRepository laboratorioRepo;

    @Autowired
    private ReservaRepository reservaRepo;

    private Long labId;

    @BeforeEach
    void setUp() throws Exception {
        reservaRepo.deleteAll();
        laboratorioRepo.deleteAll();
        labId = laboratorioRepo.save(new Laboratorio(null, "Lab. Redes", "Bloque A", 20, "REDES")).getId();

        mvc.perform(post("/reservas")
                .param("laboratorio.id", labId.toString())
                .param("nombreSolicitante", "Ana Torres")
                .param("correoSolicitante", "ana@udes.edu.co")
                .param("inicio", "2030-03-02T09:00")
                .param("fin", "2030-03-02T11:00"))
            .andExpect(redirectedUrl("/reservas"))
            .andExpect(flash().attribute("mensaje", "Reserva creada correctamente"));
    }

    @Test
    void solapamientoEnMvcRedirigeAlFormularioConElMismoMensaje() throws Exception {
        mvc.perform(post("/reservas")
                .param("laboratorio.id", labId.toString())
                .param("nombreSolicitante", "Luis Gómez")
                .param("correoSolicitante", "luis@udes.edu.co")
                .param("inicio", "2030-03-02T10:00")
                .param("fin", "2030-03-02T12:00"))
            .andExpect(redirectedUrl("/reservas/nueva"))
            .andExpect(flash().attribute("error", MENSAJE_CONFLICTO));
    }

    @Test
    void solapamientoEnRestRespondeConflictoConElMismoMensaje() throws Exception {
        String cuerpo = """
            {"laboratorio":{"id":%d},"nombreSolicitante":"Luis Gómez","correoSolicitante":"luis@udes.edu.co",
             "inicio":"2030-03-02T10:00:00","fin":"2030-03-02T12:00:00"}
            """.formatted(labId);

        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value(MENSAJE_CONFLICTO));
    }

    @Test
    void formularioConCamposInvalidosVuelveALaVista() throws Exception {
        mvc.perform(post("/reservas")
                .param("laboratorio.id", labId.toString())
                .param("nombreSolicitante", "")
                .param("correoSolicitante", "no-es-correo"))
            .andExpect(status().isOk())
            .andExpect(view().name("reservas/nueva"))
            .andExpect(model().attributeHasFieldErrors("reserva", "nombreSolicitante", "correoSolicitante"));
    }

    @Test
    void cancelarReservaInexistenteRedirigeAlListado() throws Exception {
        mvc.perform(post("/reservas/999/cancelar"))
            .andExpect(redirectedUrl("/reservas"))
            .andExpect(flash().attribute("error", "Reserva no encontrada: 999"));
    }
}
