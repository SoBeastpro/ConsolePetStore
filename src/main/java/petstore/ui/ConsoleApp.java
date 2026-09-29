package petstore.ui;

import petstore.exception.BusinessException;
import petstore.exception.EntityNotFoundException;
import petstore.model.ReservationStatus;
import petstore.repository.EntityRepository;
import petstore.repository.JdbcClientRepository;
import petstore.repository.JdbcReservationRepository;
import petstore.repository.ReservationRepository;
import petstore.service.ClientService;
import petstore.service.ReservationService;
import petstore.model.Client;
import petstore.util.DatabaseManager;
import petstore.util.ExcelExporter;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

public class ConsoleApp {
    private final ConsoleInput input = new ConsoleInput();
    private final ClientService clientService;
    private final ReservationService reservationService;
    private final ExcelExporter excelExporter = new ExcelExporter();
    private final ClientMenu clientMenu;
    private final ReservationMenu reservationMenu;

    public ConsoleApp() {
        EntityRepository<Client> clients = new JdbcClientRepository();
        ReservationRepository reservations = new JdbcReservationRepository();
        this.clientService = new ClientService(clients, reservations);
        this.reservationService = new ReservationService(clients, reservations);
        this.clientMenu = new ClientMenu(input, clientService);
        this.reservationMenu = new ReservationMenu(input, reservationService, clientService);
    }

    public void run() {
        try {
            DatabaseManager.init();
        } catch (SQLException exception) {
            SqlErrors.print(exception);
            input.close();
            return;
        }
        try {
            loop();
        } finally {
            input.close();
        }
    }

    private void loop() {
        while (true) {
            System.out.println();
            System.out.println("======================================== ПИТОМНИК ЖИВОТНЫХ ========================================");
            System.out.println("1. Клиенты");
            System.out.println("2. Заявки на резерв");
            System.out.println("3. Поиск");
            System.out.println("4. Фильтрация");
            System.out.println("5. Статистика");
            System.out.println("6. Экспорт данных");
            System.out.println("7. Вывести таблицы базы данных");
            System.out.println("0. Выход");
            int choice = input.readInt("Выберите действие: ");
            try {
                switch (choice) {
                    case 1 -> clientMenu.show();
                    case 2 -> reservationMenu.show();
                    case 3 -> search();
                    case 4 -> filter();
                    case 5 -> statistics();
                    case 6 -> export();
                    case 7 -> tables();
                    case 0 -> {
                        System.out.println("Выход из программы...");
                        return;
                    }
                    default -> System.out.println("Неверный выбор. Введите число от 0 до 7.");
                }
            } catch (EntityNotFoundException | BusinessException exception) {
                System.out.println("Ошибка: " + exception.getMessage());
            } catch (SQLException exception) {
                SqlErrors.print(exception);
            } catch (IOException exception) {
                System.out.println("Ошибка экспорта: " + exception.getMessage());
            }
        }
    }

    private void search() {
        while (true) {
            System.out.println();
            System.out.println("Поиск");
            System.out.println("1. По кличке животного");
            System.out.println("2. По ФИО клиента");
            System.out.println("0. Назад");
            int choice = input.readInt("Выберите действие: ");
            try {
                switch (choice) {
                    case 1 -> {
                        String query = input.readLine("Кличка: ");
                        Printer.reservations(reservationService.searchByAnimalName(query), clientService.namesById());
                    }
                    case 2 -> {
                        String query = input.readLine("ФИО клиента: ");
                        Printer.reservations(reservationService.searchByClientName(query), clientService.namesById());
                    }
                    case 0 -> {
                        return;
                    }
                    default -> System.out.println("Неверный выбор. Введите число от 0 до 2.");
                }
            } catch (EntityNotFoundException | BusinessException exception) {
                System.out.println("Ошибка: " + exception.getMessage());
            } catch (SQLException exception) {
                SqlErrors.print(exception);
            }
        }
    }

    private void filter() {
        while (true) {
            System.out.println();
            System.out.println("Фильтрация");
            System.out.println("1. По статусу");
            System.out.println("2. По диапазону дат создания");
            System.out.println("0. Назад");
            int choice = input.readInt("Выберите действие: ");
            try {
                switch (choice) {
                    case 1 -> {
                        String status = input.readLine("Статус (CREATED, CONFIRMED, COMPLETED, CANCELLED): ");
                        Printer.reservations(reservationService.filterByStatus(status), clientService.namesById());
                    }
                    case 2 -> {
                        LocalDate from = input.readDate("Дата с (ГГГГ-ММ-ДД): ");
                        LocalDate to = input.readDate("Дата по (ГГГГ-ММ-ДД): ");
                        Printer.reservations(reservationService.filterByCreatedAt(from, to), clientService.namesById());
                    }
                    case 0 -> {
                        return;
                    }
                    default -> System.out.println("Неверный выбор. Введите число от 0 до 2.");
                }
            } catch (EntityNotFoundException | BusinessException exception) {
                System.out.println("Ошибка: " + exception.getMessage());
            } catch (SQLException exception) {
                SqlErrors.print(exception);
            }
        }
    }

    private void statistics() throws SQLException {
        Map<ReservationStatus, Long> byStatus = reservationService.countByStatus();
        System.out.println("Всего клиентов: " + clientService.count());
        System.out.println("Всего заявок: " + reservationService.count());
        System.out.println("Созданных: " + byStatus.getOrDefault(ReservationStatus.CREATED, 0L));
        System.out.println("Подтверждённых: " + byStatus.getOrDefault(ReservationStatus.CONFIRMED, 0L));
        System.out.println("Завершённых: " + byStatus.getOrDefault(ReservationStatus.COMPLETED, 0L));
        System.out.println("Отменённых: " + byStatus.getOrDefault(ReservationStatus.CANCELLED, 0L));
    }

    private void export() throws SQLException, IOException {
        String name = input.readLine("Имя файла (Enter — petstore.xlsx): ");
        if (name.isBlank()) {
            name = "petstore.xlsx";
        }
        if (!name.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            name = name + ".xlsx";
        }
        Path file = Path.of(name);
        excelExporter.export(clientService.getAll(), reservationService.getAll(), file);
        System.out.println("Данные сохранены в файл: " + file.toAbsolutePath());
    }

    private void tables() throws SQLException {
        System.out.println("Таблица clients");
        Printer.clientTable(clientService.getAll());
        System.out.println();
        System.out.println("Таблица reservations");
        Printer.reservationTable(reservationService.getAll());
    }
}
