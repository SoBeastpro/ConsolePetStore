package petstore.ui;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class ConsoleInput {
    private final Scanner scanner = new Scanner(System.in);

    public int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException exception) {
                System.out.println("Ошибка: введите целое число.");
            }
        }
    }

    public long readId(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                return Long.parseLong(line);
            } catch (NumberFormatException exception) {
                System.out.println("Ошибка: ID должен быть целым числом.");
            }
        }
    }

    public String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                return LocalDate.parse(line);
            } catch (DateTimeParseException exception) {
                System.out.println("Ошибка: дата должна быть в формате ГГГГ-ММ-ДД.");
            }
        }
    }

    public void close() {
        scanner.close();
    }
}
