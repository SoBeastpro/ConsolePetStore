package petstore.ui;

import petstore.model.Client;
import petstore.model.Reservation;

import java.util.List;
import java.util.Map;

public final class Printer {
    private Printer() {
    }

    public static void clients(List<Client> clients) {
        if (clients.isEmpty()) {
            System.out.println("Записей нет.");
            return;
        }
        System.out.printf("%-6s %-24s %-16s %-24s%n", "ID", "ФИО", "Телефон", "Email");
        for (Client client : clients) {
            System.out.printf("%-6d %-24s %-16s %-24s%n",
                    client.getId(), client.getFullName(), client.getPhone(), client.getEmail());
        }
    }

    public static void clientTable(List<Client> clients) {
        System.out.printf("%-6s %-24s %-16s %-24s%n", "id", "full_name", "phone", "email");
        for (Client client : clients) {
            System.out.printf("%-6d %-24s %-16s %-24s%n",
                    client.getId(), client.getFullName(), client.getPhone(), client.getEmail());
        }
    }

    public static void reservations(List<Reservation> reservations, Map<Long, String> clientNames) {
        if (reservations.isEmpty()) {
            System.out.println("Записей нет.");
            return;
        }
        System.out.printf("%-6s %-22s %-16s %-12s %-12s %-24s%n",
                "ID", "Клиент", "Кличка", "Статус", "Дата", "Комментарий");
        for (Reservation reservation : reservations) {
            System.out.printf("%-6d %-22s %-16s %-12s %-12s %-24s%n",
                    reservation.getId(),
                    clientNames.getOrDefault(reservation.getClientId(), ""),
                    reservation.getAnimalName(),
                    reservation.getStatus(),
                    reservation.getCreatedAt(),
                    comment(reservation));
        }
    }

    public static void reservationTable(List<Reservation> reservations) {
        System.out.printf("%-6s %-10s %-16s %-12s %-12s %-24s%n",
                "id", "client_id", "animal_name", "status", "created_at", "comment");
        for (Reservation reservation : reservations) {
            System.out.printf("%-6d %-10d %-16s %-12s %-12s %-24s%n",
                    reservation.getId(),
                    reservation.getClientId(),
                    reservation.getAnimalName(),
                    reservation.getStatus(),
                    reservation.getCreatedAt(),
                    comment(reservation));
        }
    }

    private static String comment(Reservation reservation) {
        if (reservation.getComment() == null || reservation.getComment().isBlank()) {
            return "-";
        }
        return reservation.getComment();
    }
}
