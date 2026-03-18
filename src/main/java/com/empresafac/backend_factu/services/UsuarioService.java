package com.empresafac.backend_factu.services;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.empresafac.backend_factu.entities.Empresa;
import com.empresafac.backend_factu.entities.Usuario;
import com.empresafac.backend_factu.repositories.EmpresaRepository;
import com.empresafac.backend_factu.repositories.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlanValidadorService planValidador; // ✅ inyectado

    @Transactional
    public Usuario crearUsuario(
            String nombre,
            String username,
            String email,
            String password,
            Usuario.Rol rol,
            Long empresaIdActual) {

        // ✅ Validar límite del plan antes de crear
        planValidador.validarLimiteUsuarios(empresaIdActual);

        if (usuarioRepository.existsByUsernameAndEmpresaId(username, empresaIdActual)) {
            throw new RuntimeException("Username ya existe en la empresa");
        }

        if (usuarioRepository.existsByEmailAndEmpresaId(email, empresaIdActual)) {
            throw new RuntimeException("Email ya existe en la empresa");
        }

        Empresa empresa = empresaRepository.findById(empresaIdActual)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        Usuario usuario = new Usuario();
        usuario.setEmpresa(empresa);
        usuario.setNombre(nombre);
        usuario.setUsername(username);
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setRol(rol);
        usuario.setActivo(true);

        return usuarioRepository.save(usuario);
    }

    public List<Usuario> listarPorEmpresa(Long empresaId) {
        return usuarioRepository.findAllByEmpresaId(empresaId);
    }

    public Usuario obtenerPorId(Long empresaId, Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!usuario.getEmpresa().getId().equals(empresaId)) {
            throw new RuntimeException("No pertenece a la empresa");
        }

        return usuario;
    }

    @Transactional
    public Usuario actualizarUsuario(
            Long empresaId,
            Long usuarioId,
            String nombre,
            String username,
            String email,
            String password,
            Usuario.Rol rol,
            Boolean activo) {

        Usuario usuario = obtenerPorId(empresaId, usuarioId);

        if (!usuario.getUsername().equals(username)
                && usuarioRepository.existsByUsernameAndEmpresaId(username, empresaId)) {
            throw new RuntimeException("Username ya existe en la empresa");
        }

        if (!usuario.getEmail().equals(email)
                && usuarioRepository.existsByEmailAndEmpresaId(email, empresaId)) {
            throw new RuntimeException("Email ya existe en la empresa");
        }

        usuario.setNombre(nombre);
        usuario.setUsername(username);
        usuario.setEmail(email);
        usuario.setRol(rol);

        if (password != null && !password.isBlank()) {
            usuario.setPassword(passwordEncoder.encode(password));
        }

        if (activo != null) {
            usuario.setActivo(activo);
        }

        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void desactivarUsuario(Long empresaId, Long usuarioId) {
        Usuario usuario = obtenerPorId(empresaId, usuarioId);
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void activarUsuario(Long empresaId, Long usuarioId) {
        Usuario usuario = obtenerPorId(empresaId, usuarioId);
        usuario.setActivo(true);
        usuarioRepository.save(usuario);
    }
}