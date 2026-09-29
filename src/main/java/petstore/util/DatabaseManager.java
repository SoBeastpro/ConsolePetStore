package petstore.util;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class DatabaseManager {
    private static final String URL = "jdbc:postgresql://localhost:5432/PetStore";
    private static final String USER = "postgres";
    private static final String PASSWORD = "7g0tArk";

    private static final String CREATE_CLIENTS = """
            CREATE TABLE IF NOT EXISTS clients (
                id BIGSERIAL PRIMARY KEY,
                full_name VARCHAR(100) NOT NULL,
                phone VARCHAR(20) NOT NULL UNIQUE,
                email VARCHAR(100) NOT NULL UNIQUE
            )
            """;

    private static final String CREATE_RESERVATIONS = """
            CREATE TABLE IF NOT EXISTS reservations (
                id BIGSERIAL PRIMARY KEY,
                client_id BIGINT NOT NULL REFERENCES clients (id),
                animal_name VARCHAR(100) NOT NULL,
                status VARCHAR(20) NOT NULL CHECK (status IN ('CREATED', 'CONFIRMED', 'COMPLETED', 'CANCELLED')),
                created_at DATE NOT NULL,
                comment VARCHAR(500)
            )
            """;

    private DatabaseManager() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void init() throws SQLException {
        DriverManager.setLoginTimeout(5);
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(CREATE_CLIENTS);
                    statement.execute(CREATE_RESERVATIONS);
                }
                if (clientsEmpty(connection)) {
                    seed(connection);
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private static boolean clientsEmpty(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM clients")) {
            resultSet.next();
            return resultSet.getInt(1) == 0;
        }
    }

    private static void seed(Connection connection) throws SQLException {
        List<Long> clientIds = insertClients(connection);
        insertReservations(connection, clientIds);
    }

    private static List<Long> insertClients(Connection connection) throws SQLException {
        String sql = "INSERT INTO clients(full_name, phone, email) VALUES (?, ?, ?)";
        String[][] rows = {
                {"Иванова Анна", "+79001111111", "anna@mail.ru"},
                {"Петров Сергей", "+79002222222", "sergey@mail.ru"},
                {"Сидорова Мария", "+79003333333", "maria@mail.ru"},
                {"Кузнецов Илья", "+79004444444", "ilya@mail.ru"},
                {"Смирнова Ольга", "+79005555555", "olga@mail.ru"}
        };
        List<Long> ids = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"})) {
            for (String[] row : rows) {
                statement.setString(1, row[0]);
                statement.setString(2, row[1]);
                statement.setString(3, row[2]);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("Не удалось получить ID клиента.");
                    }
                    ids.add(keys.getLong(1));
                }
            }
        }
        return ids;
    }

    private static void insertReservations(Connection connection, List<Long> clientIds) throws SQLException {
        String sql = """
                INSERT INTO reservations(client_id, animal_name, status, created_at, comment)
                VALUES (?, ?, ?, ?, ?)
                """;
        List<SeedReservation> rows = List.of(
                new SeedReservation(0, "Барсик", "CREATED", LocalDate.of(2026, 1, 10), "Резерв кота"),
                new SeedReservation(0, "Мурка", "CONFIRMED", LocalDate.of(2026, 1, 20), "Ожидает выдачи"),
                new SeedReservation(1, "Рекс", "COMPLETED", LocalDate.of(2026, 2, 1), "Собака уже забрана"),
                new SeedReservation(1, "Шарик", "CANCELLED", LocalDate.of(2026, 2, 15), "Клиент отказался"),
                new SeedReservation(2, "Лайка", "CREATED", LocalDate.of(2026, 3, 1), "Резерв щенка"),
                new SeedReservation(2, "Бобик", "CONFIRMED", LocalDate.of(2026, 3, 18), "Подтвержден осмотр"),
                new SeedReservation(3, "Кеша", "COMPLETED", LocalDate.of(2026, 4, 2), "Попугай передан"),
                new SeedReservation(3, "Гоша", "CREATED", LocalDate.of(2026, 4, 25), "Второй резерв"),
                new SeedReservation(4, "Тузик", "CONFIRMED", LocalDate.of(2026, 5, 6), "Ожидает выдачи"),
                new SeedReservation(4, "Джесси", "CANCELLED", LocalDate.of(2026, 6, 1), "Не подошел по характеру")
        );
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (SeedReservation row : rows) {
                statement.setLong(1, clientIds.get(row.clientIndex()));
                statement.setString(2, row.animalName());
                statement.setString(3, row.status());
                statement.setDate(4, Date.valueOf(row.createdAt()));
                statement.setString(5, row.comment());
                statement.executeUpdate();
            }
        }
    }

    private record SeedReservation(int clientIndex, String animalName, String status,
                                   LocalDate createdAt, String comment) {
    }
}
