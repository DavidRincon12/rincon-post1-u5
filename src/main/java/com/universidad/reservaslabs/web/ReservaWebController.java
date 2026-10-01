package com.universidad.reservaslabs.web;

import com.universidad.reservaslabs.model.Reserva;
import com.universidad.reservaslabs.repository.LaboratorioRepository;
import com.universidad.reservaslabs.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/reservas")
public class ReservaWebController {

    // Mismo bean singleton que recibe ReservaController (REST): las reglas de
    // solapamiento, horario y cancelación no se repiten en esta clase.
    private final ReservaService service;
    private final LaboratorioRepository laboratorioRepo;

    public ReservaWebController(ReservaService service, LaboratorioRepository laboratorioRepo) {
        this.service = service;
        this.laboratorioRepo = laboratorioRepo;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("reservas", service.findAll());
        return "reservas/lista";
    }

    @GetMapping("/nueva")
    public String formularioNueva(Model model) {
        if (!model.containsAttribute("reserva")) {
            model.addAttribute("reserva", new Reserva());
        }
        model.addAttribute("laboratorios", laboratorioRepo.findAll());
        return "reservas/nueva";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("reserva") Reserva reserva, BindingResult binding,
                        Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("laboratorios", laboratorioRepo.findAll());
            return "reservas/nueva";
        }
        service.crear(reserva);
        redirect.addFlashAttribute("mensaje", "Reserva creada correctamente");
        return "redirect:/reservas";
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Long id, RedirectAttributes redirect) {
        service.cancelar(id);
        redirect.addFlashAttribute("mensaje", "Reserva cancelada correctamente");
        return "redirect:/reservas";
    }
}
