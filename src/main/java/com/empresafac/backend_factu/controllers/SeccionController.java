package com.empresafac.backend_factu.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.empresafac.backend_factu.dto_temp.request.SeccionRequest;
import com.empresafac.backend_factu.dto_temp.response.SeccionResponse;
import com.empresafac.backend_factu.entities.Seccion;
import com.empresafac.backend_factu.services.SeccionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/empresas/{empresaId}/secciones")
@RequiredArgsConstructor
public class SeccionController {

    private final SeccionService seccionService;

    @GetMapping
    public List<SeccionResponse> listar(@PathVariable Long empresaId) {
        return seccionService.listar(empresaId)
                .stream()
                .map(s -> new SeccionResponse(s.getId(), s.getNombre(), s.getActiva()))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public SeccionResponse obtener(@PathVariable Long empresaId,
            @PathVariable Long id) {
        Seccion s = seccionService.obtener(empresaId, id);
        return new SeccionResponse(s.getId(), s.getNombre(), s.getActiva());
    }

    @PostMapping
    public SeccionResponse crear(@PathVariable Long empresaId,
            @RequestBody SeccionRequest req) {
        Seccion s = seccionService.crear(empresaId, req.getNombre());
        return new SeccionResponse(s.getId(), s.getNombre(), s.getActiva());
    }

    @PutMapping("/{id}")
    public SeccionResponse actualizar(@PathVariable Long empresaId,
            @PathVariable Long id,
            @RequestBody SeccionRequest req) {
        Seccion s = seccionService.actualizar(empresaId, id, req.getNombre());
        return new SeccionResponse(s.getId(), s.getNombre(), s.getActiva());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long empresaId,
            @PathVariable Long id) {
        seccionService.eliminar(empresaId, id);
        return ResponseEntity.noContent().build();
    }
}
