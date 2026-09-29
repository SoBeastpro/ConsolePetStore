package ru.mirea.project.ui;

import java.sql.SQLException;

public final class SqlErrors {
    private SqlErrors() {
    }

    public static void print(SQLException exception) {
        if (isConnectionError(exception)) {
            System.out.println("Ошибка подключения к базе данных: " + exception.getMessage());
        } else {
            System.out.println("Ошибка выполнения SQL-запроса: " + exception.getMessage());
        }
    }

    private static boolean isConnectionError(SQLException exception) {
        String state = exception.getSQLState();
        if (state == null) {
            return true;
        }
        return state.startsWith("08")
                || "28P01".equals(state)
                || "28000".equals(state)
                || "3D000".equals(state);
    }
}
