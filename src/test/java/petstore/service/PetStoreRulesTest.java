package petstore.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import petstore.exception.BusinessException;
import petstore.exception.Messages;
import petstore.model.Client;
import petstore.model.Reservation;
import petstore.model.ReservationQuery;
import petstore.model.ReservationStatus;
import petstore.model.ReservationView;
import petstore.model.SortDirection;
import petstore.model.SortField;
import petstore.repository.EntityRepository;
import petstore.support.MemoryClientRepository;
import petstore.support.MemoryReservationRepository;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PetStoreRulesTest {
    private MemoryClientRepository clients;
    private MemoryReservationRepository reservations;
    private ClientService clientService;
    private ReservationService reservationService;
    private StatisticsService statisticsService;

    @BeforeEach
    void setUp() {
        clients = new MemoryClientRepository();
        reservations = new MemoryReservationRepository();
        clientService = new ClientService(clients, reservations);
        reservationService = new ReservationService(clients, reservations);
        statisticsService = new StatisticsService(clients, reservations);
    }

    @Test
    void interfaceFilesArePresent() throws Exception {
        assertNotNull(getClass().getResource("/petstore/fx/main-view.fxml"));
        assertNotNull(getClass().getResource("/petstore/fx/client-form.fxml"));
        assertNotNull(getClass().getResource("/petstore/fx/reservation-form.fxml"));
        assertNotNull(getClass().getResource("/petstore/fx/statistics-view.fxml"));
        assertNotNull(getClass().getResource("/petstore/fx/app.css"));
        try (var input = getClass().getResourceAsStream("/database.properties")) {
            assertNotNull(input);
            var properties = new java.util.Properties();
            properties.load(new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8));
            assertEquals(true, properties.getProperty("db.url").startsWith("jdbc:postgresql:"));
            assertEquals(false, properties.getProperty("db.user").isBlank());
            assertEquals(false, properties.getProperty("db.password").isBlank());
        }
    }

    @Test
    void createsClientAndRejectsDuplicatePhone() throws SQLException {
        long id = clientService.create("  Иванов Иван  ", " +79990000001 ", " ivan@mail.ru ");
        Client saved = clientService.getById(id);
        assertEquals("Иванов Иван", saved.getFullName());
        assertEquals("+79990000001", saved.getPhone());

        BusinessException duplicate = assertThrows(BusinessException.class,
                () -> clientService.create("Другой", "+79990000001", "other@mail.ru"));
        assertEquals(Messages.CLIENT_PHONE_DUPLICATE, duplicate.getMessage());
        assertEquals(1, clientService.list().size());
    }

    @Test
    void emptyNameDoesNotCreateClient() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> clientService.create("   ", "+79990000001", "ivan@mail.ru"));
        assertEquals(Messages.CLIENT_NAME_REQUIRED, exception.getMessage());
    }

    @Test
    void listsClientsByRussianNameThenId() throws SQLException {
        clientService.create("Петров Сергей", "+1", "p@mail.ru");
        clientService.create("Иванова Анна", "+2", "a@mail.ru");
        clientService.create("иванова анна", "+3", "a2@mail.ru");

        List<String> names = clientService.list().stream().map(Client::getFullName).toList();
        assertEquals(List.of("Иванова Анна", "иванова анна", "Петров Сергей"), names);
    }

    @Test
    void mapsDatabasePhoneConstraintWhenListDoesNotShowDuplicate() {
        EntityRepository<Client> hidden = new EntityRepository<>() {
            @Override
            public List<Client> findAll() {
                return List.of();
            }

            @Override
            public Optional<Client> findById(long id) {
                return Optional.empty();
            }

            @Override
            public long insert(Client entity) throws SQLException {
                throw new SQLException("duplicate key \"clients_email_key\"", "23505");
            }

            @Override
            public void update(Client entity) {
            }

            @Override
            public void delete(long id) {
            }
        };
        ClientService service = new ClientService(hidden, reservations);
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.create("Иван", "+1", "a@mail.ru"));
        assertEquals(Messages.CLIENT_EMAIL_DUPLICATE, exception.getMessage());
    }

    @Test
    void keepsOwnPhoneAndRejectsClientWithReservations() throws SQLException {
        long clientId = clientService.create("Иванова Анна", "+1", "a@mail.ru");
        clientService.update(clientId, "Иванова Анна", "+1", "anna@mail.ru");
        assertEquals("anna@mail.ru", clientService.getById(clientId).getEmail());

        reservationService.create(clientId, "Барсик", LocalDate.of(2026, 1, 10), "Резерв кота");
        BusinessException exception = assertThrows(BusinessException.class, () -> clientService.delete(clientId));
        assertEquals(Messages.CLIENT_HAS_RESERVATIONS, exception.getMessage());
        assertEquals("Иванова Анна", clientService.getById(clientId).getFullName());
    }

    @Test
    void createsReservationAsCreatedAndBlocksIllegalTransition() throws SQLException {
        long clientId = clientService.create("Иванова Анна", "+1", "a@mail.ru");
        long id = reservationService.create(clientId, " Барсик ", LocalDate.of(2026, 1, 10), " Резерв кота ");
        Reservation created = reservationService.getById(id);
        assertEquals(ReservationStatus.CREATED, created.getStatus());
        assertEquals("Барсик", created.getAnimalName());

        reservationService.update(id, clientId, "Барсик", created.getCreatedAt(), ReservationStatus.CONFIRMED, "Резерв кота");
        assertEquals(ReservationStatus.CONFIRMED, reservationService.getById(id).getStatus());
        assertEquals("Резерв кота", created.getComment());

        long fresh = reservationService.create(clientId, "Рекс", LocalDate.of(2026, 2, 1), "");
        BusinessException exception = assertThrows(BusinessException.class, () -> reservationService.update(
                fresh, clientId, "Рекс", LocalDate.of(2026, 2, 1), ReservationStatus.COMPLETED, ""));
        assertEquals(Messages.statusTransition("Создана", "Завершена"), exception.getMessage());
        assertEquals(ReservationStatus.CREATED, reservationService.getById(fresh).getStatus());
    }

    @Test
    void searchesFiltersAndSortsReservationsTogether() throws SQLException {
        long anna = clientService.create("Иванова Анна", "+1", "a@mail.ru");
        long petr = clientService.create("Петров Сергей", "+2", "p@mail.ru");
        reservationService.create(anna, "Барсик", LocalDate.of(2026, 1, 10), "Резерв кота");
        long confirmed = reservationService.create(anna, "Ася", LocalDate.of(2026, 3, 1), "осмотр");
        reservationService.update(confirmed, anna, "Ася", LocalDate.of(2026, 3, 1), ReservationStatus.CONFIRMED, "осмотр");
        long sameName = reservationService.create(petr, "Мурка", LocalDate.of(2026, 3, 20), "вторая");
        reservationService.update(sameName, petr, "Мурка", LocalDate.of(2026, 3, 20), ReservationStatus.CONFIRMED, "вторая");

        List<ReservationView> found = reservationService.list(new ReservationQuery("бар", null, null, null, null, null));
        assertEquals(List.of("Барсик"), found.stream().map(ReservationView::animalName).toList());

        List<ReservationView> byClient = reservationService.list(new ReservationQuery("ивано", null, null, null, null, null));
        assertEquals(2, byClient.size());

        List<ReservationView> filtered = reservationService.list(new ReservationQuery(
                null, ReservationStatus.CONFIRMED, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
                SortField.ANIMAL_NAME, SortDirection.DESC));
        assertEquals(List.of(sameName, confirmed), filtered.stream().map(ReservationView::id).toList());

        BusinessException incomplete = assertThrows(BusinessException.class, () -> reservationService.list(
                new ReservationQuery(null, null, LocalDate.of(2026, 3, 1), null, null, null)));
        assertEquals(Messages.DATE_RANGE_INCOMPLETE, incomplete.getMessage());

        BusinessException invalid = assertThrows(BusinessException.class, () -> reservationService.list(
                new ReservationQuery(null, null, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 3, 1), null, null)));
        assertEquals(Messages.DATE_RANGE_INVALID, invalid.getMessage());

        BusinessException empty = assertThrows(BusinessException.class, () -> reservationService.list(
                new ReservationQuery("   ", null, null, null, null, null)));
        assertEquals(Messages.SEARCH_EMPTY, empty.getMessage());
    }

    @Test
    void calculatesShareAndPeriodWithoutChangingTotals() throws SQLException {
        long clientId = clientService.create("Иванова Анна", "+1", "a@mail.ru");
        long first = reservationService.create(clientId, "Ася", LocalDate.of(2026, 1, 10), "");
        long second = reservationService.create(clientId, "Боря", LocalDate.of(2026, 2, 1), "");
        long third = reservationService.create(clientId, "Вера", LocalDate.of(2026, 3, 1), "");
        reservationService.update(second, clientId, "Боря", LocalDate.of(2026, 2, 1), ReservationStatus.CONFIRMED, "");
        reservationService.update(second, clientId, "Боря", LocalDate.of(2026, 2, 1), ReservationStatus.COMPLETED, "");
        reservationService.update(third, clientId, "Вера", LocalDate.of(2026, 3, 1), ReservationStatus.CONFIRMED, "");
        reservationService.update(third, clientId, "Вера", LocalDate.of(2026, 3, 1), ReservationStatus.COMPLETED, "");

        var report = statisticsService.report(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        assertEquals(1, report.clientCount());
        assertEquals(3, report.reservationCount());
        assertEquals(1, report.createdCount());
        assertEquals(2, report.completedCount());
        assertEquals(new BigDecimal("66.7"), report.completedShare());
        assertEquals(LocalDate.of(2026, 1, 10), report.earliestCreatedAt());
        assertEquals(LocalDate.of(2026, 3, 1), report.latestCreatedAt());
        assertEquals(1L, report.reservationsInPeriod());
        assertEquals(first, reservationService.getById(first).getId());

        var empty = new StatisticsService(new MemoryClientRepository(), new MemoryReservationRepository()).report(null, null);
        assertEquals(new BigDecimal("0.0"), empty.completedShare());
        assertNull(empty.earliestCreatedAt());
        assertNull(empty.reservationsInPeriod());
    }

    @Test
    void exportsCurrentRowsWithRussianStatus() throws Exception {
        Client client = new Client(5, "Иванова Анна", "+1", "a@mail.ru");
        ReservationView reservation = new ReservationView(
                8, 5, "Иванова Анна", "Рекс", ReservationStatus.CONFIRMED, LocalDate.of(2026, 3, 1), "");
        Path file = Files.createTempFile("petstore-", ".xlsx");
        try {
            new ExportService().exportXlsx(file, List.of(client), List.of(reservation));
            try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(file))) {
                Sheet clientsSheet = workbook.getSheet("Клиенты");
                assertEquals("ФИО", clientsSheet.getRow(0).getCell(1).getStringCellValue());
                assertEquals("Иванова Анна", clientsSheet.getRow(1).getCell(1).getStringCellValue());

                Sheet reservationsSheet = workbook.getSheet("Заявки");
                assertEquals("Комментарий", reservationsSheet.getRow(0).getCell(5).getStringCellValue());
                assertEquals("Подтверждена", reservationsSheet.getRow(1).getCell(3).getStringCellValue());
                assertEquals("", reservationsSheet.getRow(1).getCell(5).getStringCellValue());
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
