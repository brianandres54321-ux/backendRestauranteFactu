package com.empresafac.backend_factu.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.empresafac.backend_factu.entities.Categoria;
import com.empresafac.backend_factu.entities.Empresa;
import com.empresafac.backend_factu.repositories.CategoriaRepository;
import com.empresafac.backend_factu.repositories.EmpresaRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final EmpresaRepository empresaRepository;

    /*
     * Crea una nueva categoría dentro de la empresa indicada.
     */
    @Transactional
    public Categoria crear(Long empresaId, String nombre) {

        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        Categoria categoria = new Categoria();
        categoria.setEmpresa(empresa);
        categoria.setNombre(nombre);
        categoria.setActiva(true);
 
        return categoriaRepository.save(categoria);
    }

    /**
     * Lista categorías activas de una empresa.
     */
    public List<Categoria> listar(Long empresaId) {
        return categoriaRepository.findAllByEmpresaIdAndActivaTrue(empresaId);
    }

    /**
     * Obtiene una categoría válida y activa por id/empresa.
     */
    public Categoria obtener(Long empresaId, Long categoriaId) {
        return categoriaRepository
                .findByIdAndEmpresaIdAndActivaTrue(categoriaId, empresaId)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
    }

    /**
     * Actualiza el nombre de la categoría especificada.
     */
    @Transactional
    public Categoria actualizar(Long empresaId, Long categoriaId, String nuevoNombre) {
        Categoria categoria = categoriaRepository
                .findByIdAndEmpresaIdAndActivaTrue(categoriaId, empresaId)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
        categoria.setNombre(nuevoNombre);
        return categoriaRepository.save(categoria);
    }

    /**
     * Desactiva (soft delete) la categoría.
     */
    @Transactional
    public void eliminar(Long empresaId, Long categoriaId) {
        Categoria categoria = categoriaRepository
                .findByIdAndEmpresaIdAndActivaTrue(categoriaId, empresaId)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
        categoria.setActiva(false);
        categoriaRepository.save(categoria);
    }
}
