package com.empresafac.backend_factu.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.empresafac.backend_factu.entities.Empresa;
import com.empresafac.backend_factu.repositories.EmpresaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class EmpresaService {

    private final EmpresaRepository empresaRepository;

    /**
     * Crea una empresa nueva. No valida ningún campo adicional por ahora.
     */
    public Empresa crear(Empresa empresa) {
        return empresaRepository.save(empresa);
    }

    /**
     * Lista únicamente las empresas activas.
     */
    public List<Empresa> listar() {
        return empresaRepository.findAllByActivaTrue();
    }

    /**
     * Busca una empresa activa por id.
     */
    public Optional<Empresa> buscarPorId(Long id) {
        return empresaRepository.findByIdAndActivaTrue(id);
    }

    /**
     * Actualiza datos básicos de una empresa activa. Devuelve la entidad
     * modificada.
     */
    @Transactional
    public Empresa actualizar(Long id, Empresa datos) {
        Empresa existente = empresaRepository.findByIdAndActivaTrue(id)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));
        // copiar campos que se permiten cambiar
        existente.setNombre(datos.getNombre());
        existente.setNitRut(datos.getNitRut());
        existente.setPlan(datos.getPlan());
        // no tocamos creadaEn o id
        return empresaRepository.save(existente);
    }

    /**
     * Desactiva (soft delete) la empresa.
     */
    @Transactional
    public void eliminar(Long id) {
        Empresa existente = empresaRepository.findByIdAndActivaTrue(id)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));
        existente.setActiva(false);
        empresaRepository.save(existente);
    }
}
