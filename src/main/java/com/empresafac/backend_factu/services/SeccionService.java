package com.empresafac.backend_factu.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.empresafac.backend_factu.entities.Empresa;
import com.empresafac.backend_factu.entities.Seccion;
import com.empresafac.backend_factu.repositories.EmpresaRepository;
import com.empresafac.backend_factu.repositories.SeccionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor 
public class SeccionService {

    private final SeccionRepository seccionRepository;
    private final EmpresaRepository empresaRepository;

    /*
     * Crea una nueva sección asociada a una empresa. La empresa debe existir, y la
     * sección se marca como activa por defecto.
     */
    @Transactional
    public Seccion crear(Long empresaId, String nombre) {

        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        Seccion seccion = new Seccion();
        seccion.setEmpresa(empresa);
        seccion.setNombre(nombre); 
        seccion.setActiva(true);

        return seccionRepository.save(seccion);
    }

    /**
     * Lista todas las secciones activas de una empresa.
     */
    public List<Seccion> listar(Long empresaId) {
        return seccionRepository.findAllByEmpresaIdAndActivaTrue(empresaId);
    }

    /**
     * Recupera una sección específica perteneciente a la empresa. Lanza
     * excepción si no existe o está inactiva.
     */
    public Seccion obtener(Long empresaId, Long seccionId) {
        return seccionRepository
                .findByIdAndEmpresaIdAndActivaTrue(seccionId, empresaId)
                .orElseThrow(() -> new RuntimeException("Sección no encontrada"));
    }

    /**
     * Actualiza el nombre de la sección indicada (debe pertenecer a la empresa
     * y estar activa).
     */
    @Transactional
    public Seccion actualizar(Long empresaId, Long seccionId, String nuevoNombre) {
        Seccion seccion = seccionRepository
                .findByIdAndEmpresaIdAndActivaTrue(seccionId, empresaId)
                .orElseThrow(() -> new RuntimeException("Sección no encontrada"));
        seccion.setNombre(nuevoNombre);
        return seccionRepository.save(seccion);
    }

    /**
     * Marca la sección como inactiva (soft delete). No elimina físicamente.
     */
    @Transactional
    public void eliminar(Long empresaId, Long seccionId) {
        Seccion seccion = seccionRepository
                .findByIdAndEmpresaIdAndActivaTrue(seccionId, empresaId)
                .orElseThrow(() -> new RuntimeException("Sección no encontrada"));
        seccion.setActiva(false);
        seccionRepository.save(seccion);
    }
}
