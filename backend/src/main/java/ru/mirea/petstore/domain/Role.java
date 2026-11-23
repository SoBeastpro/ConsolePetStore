package ru.mirea.petstore.domain;

/**
 * Роли прикладного доступа. Сервер проверяет их независимо от того, какие кнопки
 * скрыл конкретный клиент.
 */
public enum Role {
    VIEWER,
    KEEPER,
    ADMIN
}
