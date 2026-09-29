package ru.mirea.project.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface EntityRepository<T> {
    List<T> findAll() throws SQLException;

    Optional<T> findById(long id) throws SQLException;

    long insert(T entity) throws SQLException;

    void update(T entity) throws SQLException;

    void delete(long id) throws SQLException;
}
