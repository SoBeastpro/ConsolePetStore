package ru.mirea.petstore.error;

/** Сигнал бизнес-конфликта или устаревшей версии объекта. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}
