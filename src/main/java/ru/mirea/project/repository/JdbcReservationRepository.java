package ru.mirea.project.repository;

import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.Reservation;
import ru.mirea.project.model.ReservationStatus;
import ru.mirea.project.util.DatabaseManager;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcReservationRepository implements ReservationRepository {
    @Override
    public List<Reservation> findAll() throws SQLException {
        String sql = """
                SELECT id, client_id, animal_name, status, created_at, comment
                FROM reservations
                ORDER BY id
                """;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Reservation> reservations = new ArrayList<>();
            while (resultSet.next()) {
                reservations.add(map(resultSet));
            }
            return reservations;
        }
    }

    @Override
    public Optional<Reservation> findById(long id) throws SQLException {
        String sql = """
                SELECT id, client_id, animal_name, status, created_at, comment
                FROM reservations
                WHERE id = ?
                """;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    @Override
    public long insert(Reservation reservation) throws SQLException {
        String sql = """
                INSERT INTO reservations(client_id, animal_name, status, created_at, comment)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"})) {
            bind(statement, reservation);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    reservation.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("Не удалось сохранить заявку.");
    }

    @Override
    public void update(Reservation reservation) throws SQLException {
        String sql = """
                UPDATE reservations
                SET client_id = ?, animal_name = ?, status = ?, created_at = ?, comment = ?
                WHERE id = ?
                """;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, reservation);
            statement.setLong(6, reservation.getId());
            if (statement.executeUpdate() == 0) {
                throw new EntityNotFoundException("Заявка с ID " + reservation.getId() + " не найдена.");
            }
        }
    }

    @Override
    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM reservations WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            if (statement.executeUpdate() == 0) {
                throw new EntityNotFoundException("Заявка с ID " + id + " не найдена.");
            }
        }
    }

    @Override
    public int countByClientId(long clientId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reservations WHERE client_id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, clientId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private void bind(PreparedStatement statement, Reservation reservation) throws SQLException {
        statement.setLong(1, reservation.getClientId());
        statement.setString(2, reservation.getAnimalName());
        statement.setString(3, reservation.getStatus().name());
        statement.setDate(4, Date.valueOf(reservation.getCreatedAt()));
        statement.setString(5, reservation.getComment());
    }

    private Reservation map(ResultSet resultSet) throws SQLException {
        String comment = resultSet.getString("comment");
        return new Reservation(
                resultSet.getLong("id"),
                resultSet.getLong("client_id"),
                resultSet.getString("animal_name"),
                ReservationStatus.valueOf(resultSet.getString("status")),
                resultSet.getDate("created_at").toLocalDate(),
                comment == null ? "" : comment
        );
    }
}
