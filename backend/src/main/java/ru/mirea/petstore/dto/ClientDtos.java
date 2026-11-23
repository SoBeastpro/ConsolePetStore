package ru.mirea.petstore.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/** DTO клиентов питомника. */
public final class ClientDtos {
    private ClientDtos() {
    }

    public record ClientRequest(
            @NotBlank @Size(max = 50) String code,
            @NotBlank @Size(max = 100) String fullName,
            @NotBlank @Size(max = 20) @Pattern(regexp = "^[+0-9()\\- ]{7,20}$", message = "некорректный телефон") String phone,
            @NotBlank @Email @Size(max = 100) String email,
            Boolean active,
            Long version) {
    }

    public record ClientResponse(
            UUID id,
            String code,
            String fullName,
            String phone,
            String email,
            boolean active,
            Instant createdAt,
            Instant updatedAt,
            long version) {
    }
}
