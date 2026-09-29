package petstore.ui;

import petstore.exception.BusinessException;
import petstore.exception.EntityNotFoundException;
import petstore.model.Client;
import petstore.service.ClientService;

import java.sql.SQLException;
import java.util.List;

public class ClientMenu {
    private final ConsoleInput input;
    private final ClientService clientService;

    public ClientMenu(ConsoleInput input, ClientService clientService) {
        this.input = input;
        this.clientService = clientService;
    }

    public void show() {
        while (true) {
            System.out.println();
            System.out.println("Клиенты");
            System.out.println("1. Создать");
            System.out.println("2. Вывести все");
            System.out.println("3. Получить по ID");
            System.out.println("4. Изменить");
            System.out.println("5. Удалить");
            System.out.println("0. Назад");
            int choice = input.readInt("Выберите действие: ");
            try {
                switch (choice) {
                    case 1 -> create();
                    case 2 -> Printer.clients(clientService.getAll());
                    case 3 -> showOne();
                    case 4 -> update();
                    case 5 -> delete();
                    case 0 -> {
                        return;
                    }
                    default -> System.out.println("Неверный выбор. Введите число от 0 до 5.");
                }
            } catch (EntityNotFoundException | BusinessException exception) {
                System.out.println("Ошибка: " + exception.getMessage());
            } catch (SQLException exception) {
                SqlErrors.print(exception);
            }
        }
    }

    private void create() throws SQLException {
        String fullName = input.readLine("ФИО: ");
        String phone = input.readLine("Телефон: ");
        String email = input.readLine("Email: ");
        long id = clientService.create(fullName, phone, email);
        System.out.println("Клиент создан. ID: " + id);
    }

    private void showOne() throws SQLException {
        long id = input.readId("Введите ID: ");
        Printer.clients(List.of(clientService.getById(id)));
    }

    private void update() throws SQLException {
        long id = input.readId("Введите ID: ");
        Client client = clientService.getById(id);
        System.out.println("Текущие данные: " + client.getFullName() + ", " + client.getPhone() + ", " + client.getEmail());
        String fullName = input.readLine("ФИО: ");
        String phone = input.readLine("Телефон: ");
        String email = input.readLine("Email: ");
        clientService.update(id, fullName, phone, email);
        System.out.println("Клиент изменен.");
    }

    private void delete() throws SQLException {
        long id = input.readId("Введите ID: ");
        clientService.delete(id);
        System.out.println("Клиент удален.");
    }
}
