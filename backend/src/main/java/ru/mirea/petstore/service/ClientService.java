package ru.mirea.petstore.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.petstore.domain.ShelterClient;
import ru.mirea.petstore.dto.ClientDtos.ClientRequest;
import ru.mirea.petstore.dto.ClientDtos.ClientResponse;
import ru.mirea.petstore.error.BadRequestException;
import ru.mirea.petstore.error.ConflictException;
import ru.mirea.petstore.error.NotFoundException;
import ru.mirea.petstore.repository.ShelterClientRepository;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ClientService {
    private final ShelterClientRepository clients;
    private final AuditService audit;

    public ClientService(ShelterClientRepository clients, AuditService audit) {
        this.clients = clients;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<ClientResponse> list(boolean activeOnly) {
        return (activeOnly ? clients.findByActiveTrueOrderByFullNameAsc() : clients.findAllByOrderByFullNameAsc())
                .stream().map(ClientService::toResponse).toList();
    }

    @Transactional
    public ClientResponse create(ClientRequest request) {
        String code = normalizeRequired(request.code());
        String phone = normalizeRequired(request.phone());
        String email = normalizeEmail(request.email());
        ensureUnique(code, phone, email, null);
        ShelterClient client = new ShelterClient(UUID.randomUUID(), code, normalizeRequired(request.fullName()), phone, email);
        if (request.active() != null) client.setActive(request.active());
        clients.save(client);
        audit.record("CLIENT_CREATED", "CLIENT", client.getId(), "Создан клиент " + code, Map.of());
        return toResponse(client);
    }

    @Transactional
    public ClientResponse update(UUID id, ClientRequest request) {
        ShelterClient client = require(id);
        if (request.version() == null || request.version() != client.getVersion()) {
            throw new ConflictException("Клиент уже изменён. Обновите список");
        }
        String code = normalizeRequired(request.code());
        String phone = normalizeRequired(request.phone());
        String email = normalizeEmail(request.email());
        ensureUnique(code, phone, email, id);
        client.setCode(code);
        client.setFullName(normalizeRequired(request.fullName()));
        client.setPhone(phone);
        client.setEmail(email);
        if (request.active() != null) client.setActive(request.active());
        audit.record("CLIENT_UPDATED", "CLIENT", id, "Изменён клиент " + code, Map.of());
        return toResponse(client);
    }

    @Transactional
    public ClientResponse setActive(UUID id, boolean active, long version) {
        ShelterClient client = require(id);
        if (version != client.getVersion()) throw new ConflictException("Клиент уже изменён. Обновите список");
        client.setActive(active);
        audit.record(active ? "CLIENT_ACTIVATED" : "CLIENT_DEACTIVATED", "CLIENT", id,
                (active ? "Включён" : "Отключён") + " клиент " + client.getCode(), Map.of());
        return toResponse(client);
    }

    private void ensureUnique(String code, String phone, String email, UUID currentId) {
        clients.findByCodeIgnoreCase(code)
                .filter(other -> currentId == null || !other.getId().equals(currentId))
                .ifPresent(other -> { throw new ConflictException("Клиент с таким кодом уже существует"); });
        if (clients.findAll().stream().anyMatch(other ->
                (currentId == null || !other.getId().equals(currentId)) && other.getPhone().equals(phone))) {
            throw new ConflictException("Клиент с таким телефоном уже существует");
        }
        if (clients.findAll().stream().anyMatch(other ->
                (currentId == null || !other.getId().equals(currentId))
                        && other.getEmail().equalsIgnoreCase(email))) {
            throw new ConflictException("Клиент с таким email уже существует");
        }
    }

    private ShelterClient require(UUID id) {
        return clients.findById(id).orElseThrow(() -> new NotFoundException("Клиент не найден"));
    }

    private static ClientResponse toResponse(ShelterClient client) {
        return new ClientResponse(client.getId(), client.getCode(), client.getFullName(), client.getPhone(),
                client.getEmail(), client.isActive(), client.getCreatedAt(), client.getUpdatedAt(), client.getVersion());
    }

    private static String normalizeRequired(String value) {
        if (value == null || value.isBlank()) throw new BadRequestException("Обязательное поле не заполнено");
        return value.trim().replaceAll("\\s+", " ");
    }

    private static String normalizeEmail(String value) {
        return normalizeRequired(value).toLowerCase(Locale.ROOT);
    }
}
