package com.empresafac.backend_factu.controllers;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.empresafac.backend_factu.Security.EmpresaContext;
import com.empresafac.backend_factu.entities.Usuario;
import com.empresafac.backend_factu.services.UsuarioService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final EmpresaContext empresaContext;

    // 🔹 Crear
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Usuario crear(@RequestBody Usuario request) {

        Long empresaId = empresaContext.getEmpresaIdActual();

        return usuarioService.crearUsuario(
                request.getNombre(),
                request.getUsername(),
                request.getPassword(),
                request.getRol(),
                empresaId
        );
    }

    // 🔹 Listar
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<Usuario> listar() {
        Long empresaId = empresaContext.getEmpresaIdActual();
        return usuarioService.listarPorEmpresa(empresaId);
    }

    // 🔹 Obtener por ID
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{usuarioId}")
    public Usuario obtener(@PathVariable Long usuarioId) {
        Long empresaId = empresaContext.getEmpresaIdActual();
        return usuarioService.obtenerPorId(empresaId, usuarioId);
    }

    // 🔹 Actualizar
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{usuarioId}")
    public Usuario actualizar(
            @PathVariable Long usuarioId,
            @RequestBody Usuario request) {

        Long empresaId = empresaContext.getEmpresaIdActual();

        return usuarioService.actualizarUsuario(
                empresaId,
                usuarioId,
                request.getNombre(),
                request.getUsername(),
                request.getPassword(),
                request.getRol(),
                request.getActivo() 
        );
    }

    // 🔹 Desactivar (soft delete)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{usuarioId}")
    public void desactivar(@PathVariable Long usuarioId) {
        Long empresaId = empresaContext.getEmpresaIdActual();
        usuarioService.desactivarUsuario(empresaId, usuarioId);
    }

    // 🔹 Reactivar
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{usuarioId}/activar")
    public void activar(@PathVariable Long usuarioId) {
        Long empresaId = empresaContext.getEmpresaIdActual();
        usuarioService.activarUsuario(empresaId, usuarioId);
    }
}
