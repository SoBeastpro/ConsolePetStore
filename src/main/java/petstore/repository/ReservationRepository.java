package petstore.repository;

import petstore.model.Reservation;

import java.sql.SQLException;

public interface ReservationRepository extends EntityRepository<Reservation> {
    int countByClientId(long clientId) throws SQLException;
}
