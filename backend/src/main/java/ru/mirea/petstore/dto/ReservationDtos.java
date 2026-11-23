package ru.mirea.petstore.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.mirea.petstore.domain.Priority;
import ru.mirea.petstore.domain.ReservationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** DTO заявок и массовых операций. */
public final class ReservationDtos {
    private ReservationDtos() {
    }

    public record ReservationRequest(
            @NotBlank @Size(max = 100) String code,
            @NotBlank @Size(max = 100) String animalName,
            @Size(max = 500) String comment,
            @NotNull LocalDate reservationDate,
            @NotNull UUID clientId,
            @NotNull ReservationStatus status,
            @NotNull @DecimalMin("0.00") @DecimalMax("1000000.00") BigDecimal depositAmount,
            boolean needsCare,
            @Size(max = 20) Set<@NotBlank @Size(max = 50) String> tags,
            Long version) {
    }

    public record ReservationResponse(
            UUID id,
            String code,
            String animalName,
            String comment,
            LocalDate reservationDate,
            UUID clientId,
            String clientCode,
            String clientName,
            ReservationStatus status,
            BigDecimal depositAmount,
            boolean needsCare,
            Priority priority,
            boolean archived,
            Instant archivedAt,
            Set<String> tags,
            Instant createdAt,
            Instant updatedAt,
            long version) {
    }

    public enum BulkAction { ARCHIVE, RESTORE, SET_STATUS, ADD_TAG, REMOVE_TAG }

    public record BulkRequest(
            @NotNull @Size(min = 1, max = 500) Set<UUID> ids,
            @NotNull BulkAction action,
            ReservationStatus status,
            @Size(max = 50) String tag) {
    }

    public record BulkResult(int requested, int succeeded, List<BulkFailure> failures) {
    }

    public record BulkFailure(UUID id, String message) {
    }
}
