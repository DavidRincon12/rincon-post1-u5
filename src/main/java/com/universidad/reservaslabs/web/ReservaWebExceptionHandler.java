package com.universidad.reservaslabs.web;

import com.universidad.reservaslabs.exception.RecursoNoEncontradoException;
import com.universidad.reservaslabs.exception.ReservaConflictException;
import com.universidad.reservaslabs.exception.ReservaInvalidaException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Restringido con assignableTypes a ReservaWebController para no competir con
// GlobalRestExceptionHandler. Son las mismas excepciones de dominio que recibe
// la API REST, pero aquí se presentan como redirección con un mensaje flash.
@ControllerAdvice(assignableTypes = ReservaWebController.class)
public class ReservaWebExceptionHandler {

    private static final String LISTA = "redirect:/reservas";
    private static final String FORMULARIO = "redirect:/reservas/nueva";

    @ExceptionHandler({ReservaConflictException.class, ReservaInvalidaException.class})
    public String reglaDeNegocio(RuntimeException ex, HttpServletRequest request, RedirectAttributes redirect) {
        redirect.addFlashAttribute("error", ex.getMessage());
        // Un error al cancelar se muestra en el listado; uno al crear, en el formulario.
        return request.getRequestURI().endsWith("/cancelar") ? LISTA : FORMULARIO;
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public String noEncontrado(RecursoNoEncontradoException ex, RedirectAttributes redirect) {
        redirect.addFlashAttribute("error", ex.getMessage());
        return LISTA;
    }
}
