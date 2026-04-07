package com.agropulse.dao;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz genérica DAO (Data Access Object).
 * Define las operaciones CRUD básicas para cualquier entidad.
 * @param <T> Tipo de la entidad.
 */
public interface GenericDao<T> {

    /**
     * Guardar una nueva entidad.
     * @return El id generado.
     */
    int save(T entity);

    /**
     * Actualizar una entidad existente.
     */
    void update(T entity);

    /**
     * Eliminar por id.
     */
    void delete(int id);

    /**
     * Buscar por id.
     */
    Optional<T> findById(int id);

    /**
     * Obtener todos los registros.
     */
    List<T> findAll();
}
