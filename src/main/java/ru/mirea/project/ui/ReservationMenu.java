package ru.mirea.project.ui;

import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.Reservation;
import ru.mirea.project.service.ClientService;
import ru.mirea.project.service.ReservationService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ReservationMenu {
    private final ConsoleInput input;
    private final ReservationService reservationService;
    private final ClientService clientService;

    public ReservationMenu(ConsoleInput input, ReservationService reservationService, ClientService clientService) {
        this.input = input;
        this.reservationService = reservationService;
        this.clientService = clientService;
    }

    public void show() {
        while (true) {
            System.out.println();
            System.out.println("Заявки на резерв");
            System.out.println("1. Создать");
            System.out.println("2. Вывести все");
            System.out.println("3. Получить по ID");
            System.out.println("4. Изменить");
            System.out.println("5. Удалить");
            System.out.println("6. Сортировка по дате создания");
            System.out.println("7. Сортировка по кличке");
            System.out.println("0. Назад");
            int choice = input.readInt("Выберите действие: ");
            try {
                switch (choice) {
                    case 1 -> create();
                    case 2 -> print(reservationService.getAll());
                    case 3 -> showOne();
                    case 4 -> update();
                    case 5 -> delete();
                    case 6 -> print(reservationService.sortedByCreatedAt());
                    case 7 -> print(reservationService.sortedByAnimalName());
                    case 0 -> {
                        return;
                    }
                    default -> System.out.println("Неверный выбор. Введите число от 0 до 7.");
                }
            } catch (EntityNotFoundException | BusinessException exception) {
                System.out.println("Ошибка: " + exception.getMessage());
            } catch (SQLException exception) {
                SqlErrors.print(exception);
            }
        }
    }

    private void create() throws SQLException {
        System.out.println("Клиенты:");
        Printer.clients(clientService.getAll());
        long clientId = input.readId("ID клиента: ");
        String animalName = input.readLine("Кличка животного: ");
        LocalDate createdAt = input.readDate("Дата создания (ГГГГ-ММ-ДД): ");
        String comment = input.readLine("Комментарий: ");
        long id = reservationService.create(clientId, animalName, createdAt, comment);
        System.out.println("Заявка создана. ID: " + id + ". Статус: CREATED");
    }

    private void showOne() throws SQLException {
        long id = input.readId("Введите ID: ");
        print(List.of(reservationService.getById(id)));
    }

    private void update() throws SQLException {
        long id = input.readId("Введите ID: ");
        Reservation reservation = reservationService.getById(id);
        print(List.of(reservation));
        System.out.println("Переходы: CREATED -> CONFIRMED или CANCELLED; CONFIRMED -> COMPLETED или CANCELLED.");
        System.out.println("Клиенты:");
        Printer.clients(clientService.getAll());
        long clientId = input.readId("ID клиента: ");
        String animalName = input.readLine("Кличка животного: ");
        LocalDate createdAt = input.readDate("Дата создания (ГГГГ-ММ-ДД): ");
        String status = input.readLine("Статус (CREATED, CONFIRMED, COMPLETED, CANCELLED): ");
        String comment = input.readLine("Комментарий: ");
        reservationService.update(id, clientId, animalName, createdAt, status, comment);
        System.out.println("Заявка изменена.");
    }

    private void delete() throws SQLException {
        long id = input.readId("Введите ID: ");
        reservationService.delete(id);
        System.out.println("Заявка удалена.");
    }

    private void print(List<Reservation> reservations) throws SQLException {
        Printer.reservations(reservations, clientService.namesById());
    }
}
