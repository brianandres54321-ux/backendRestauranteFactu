package com.empresafac.backend_factu.config;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.empresafac.backend_factu.dto_temp.request.RegisterRequest;
import com.empresafac.backend_factu.entities.Empresa;
import com.empresafac.backend_factu.entities.Usuario;
import com.empresafac.backend_factu.repositories.EmpresaRepository;
import com.empresafac.backend_factu.repositories.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Registro público: crea la empresa y su primer usuario (ADMIN) en una
     * sola transacción, y devuelve un token para dejarlo logueado de una vez.
     */
    @Transactional
    public String registrar(RegisterRequest req) {

        if (empresaRepository.existsByNitRut(req.getNitRut())) {
            throw new RuntimeException("Ya existe una empresa registrada con ese NIT/RUT");
        }

        if (usuarioRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new RuntimeException("Ya existe una cuenta con ese correo");
        }

        Empresa empresa = new Empresa();
        empresa.setNombre(req.getEmpresaNombre());
        empresa.setNitRut(req.getNitRut());
        empresa.setPlan("BASICO");
        empresa.setTipoNegocio(req.getTipoNegocio());
        empresa.setActiva(true);
        empresa = empresaRepository.save(empresa);

        Usuario admin = new Usuario();
        admin.setEmpresa(empresa);
        admin.setNombre(req.getAdminNombre());
        admin.setUsername(req.getUsername());
        admin.setEmail(req.getEmail());
        admin.setPassword(passwordEncoder.encode(req.getPassword()));
        admin.setRol(Usuario.Rol.ADMIN);
        admin.setActivo(true);
        admin = usuarioRepository.save(admin);

        return jwtService.generarToken(admin);
    }

    public String login(String email, String password) {

        Usuario usuario = usuarioRepository
                .findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));

        if (!usuario.getActivo()) {
            throw new RuntimeException("Usuario inactivo");
        }

        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            throw new RuntimeException("Credenciales inválidas");
        }

        return jwtService.generarToken(usuario);
    }

    /**
     * Genera un nuevo token con el plan actualizado del usuario.
     * Se usa cuando la empresa cambia de plan, para refrescar los claims del JWT.
     */
    public String refreshToken(String username) {
        Usuario usuario = usuarioRepository
                .findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return jwtService.generarToken(usuario);
    }
}