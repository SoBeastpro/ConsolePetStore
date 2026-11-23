package ru.mirea.petstore.domain;

/** Статус заявки на резерв животного. Совпадает с предметной областью КР №1. */
public enum ReservationStatus {
    CREATED,
    CONFIRMED,
    COMPLETED,
    CANCELLED
}
