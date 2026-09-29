package petstore.repository;

import petstore.exception.EntityNotFoundException;
import petstore.model.Client;
import petstore.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcClientRepository implements EntityRepository<Client> {
    @Override
    public List<Client> findAll() throws SQLException {
        String sql = "SELECT id, full_name, phone, email FROM clients ORDER BY id";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Client> clients = new ArrayList<>();
            while (resultSet.next()) {
                clients.add(map(resultSet));
            }
            return clients;
        }
    }

    @Override
    public Optional<Client> findById(long id) throws SQLException {
        String sql = "SELECT id, full_name, phone, email FROM clients WHERE id = ?";
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
    public long insert(Client client) throws SQLException {
        String sql = "INSERT INTO clients(full_name, phone, email) VALUES (?, ?, ?)";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"})) {
            statement.setString(1, client.getFullName());
            statement.setString(2, client.getPhone());
            statement.setString(3, client.getEmail());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    client.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("Не удалось сохранить клиента.");
    }

    @Override
    public void update(Client client) throws SQLException {
        String sql = "UPDATE clients SET full_name = ?, phone = ?, email = ? WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, client.getFullName());
            statement.setString(2, client.getPhone());
            statement.setString(3, client.getEmail());
            statement.setLong(4, client.getId());
            if (statement.executeUpdate() == 0) {
                throw new EntityNotFoundException("Клиент с ID " + client.getId() + " не найден.");
            }
        }
    }

    @Override
    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM clients WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            if (statement.executeUpdate() == 0) {
                throw new EntityNotFoundException("Клиент с ID " + id + " не найден.");
            }
        }
    }

    private Client map(ResultSet resultSet) throws SQLException {
        return new Client(
                resultSet.getLong("id"),
                resultSet.getString("full_name"),
                resultSet.getString("phone"),
                resultSet.getString("email")
        );
    }
}
