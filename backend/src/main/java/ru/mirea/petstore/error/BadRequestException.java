package ru.mirea.petstore.error;

/** Сигнал некорректного прикладного запроса. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
}
