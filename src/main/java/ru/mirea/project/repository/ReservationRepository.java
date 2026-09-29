package ru.mirea.project.repository;

import ru.mirea.project.model.Reservation;

import java.sql.SQLException;

public interface ReservationRepository extends EntityRepository<Reservation> {
    int countByClientId(long clientId) throws SQLException;
}
