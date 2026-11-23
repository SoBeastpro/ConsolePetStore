package ru.mirea.petstore.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.petstore.config.AppProperties;
import ru.mirea.petstore.domain.Reservation;
import ru.mirea.petstore.domain.ShelterClient;
import ru.mirea.petstore.domain.UserAccount;
import ru.mirea.petstore.dto.ReservationDtos.BulkAction;
import ru.mirea.petstore.dto.ReservationDtos.BulkFailure;
import ru.mirea.petstore.dto.ReservationDtos.BulkRequest;
import ru.mirea.petstore.dto.ReservationDtos.BulkResult;
import ru.mirea.petstore.dto.ReservationDtos.ReservationRequest;
import ru.mirea.petstore.dto.ReservationDtos.ReservationResponse;
import ru.mirea.petstore.error.BadRequestException;
import ru.mirea.petstore.error.ConflictException;
import ru.mirea.petstore.error.NotFoundException;
import ru.mirea.petstore.filter.ReservationFilter;
import ru.mirea.petstore.filter.ReservationSpecifications;
import ru.mirea.petstore.filter.SmartFilterParser;
import ru.mirea.petstore.repository.ReservationNoteRepository;
import ru.mirea.petstore.repository.ReservationRepository;
import ru.mirea.petstore.repository.ShelterClientRepository;
import ru.mirea.petstore.security.CurrentUserService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ReservationService {
    private final ReservationRepository reservations;
    private final ReservationNoteRepository notes;
    private final ShelterClientRepository clients;
    private final CurrentUserService currentUser;
    private final SmartFilterParser parser;
    private final ReservationMapper mapper;
    private final PriorityCalculator priorityCalculator;
    private final AuditService audit;
    private final AppProperties properties;

    public ReservationService(ReservationRepository reservations, ReservationNoteRepository notes,
                              ShelterClientRepository clients, CurrentUserService currentUser,
                              SmartFilterParser parser, ReservationMapper mapper,
                              PriorityCalculator priorityCalculator, AuditService audit,
                              AppProperties properties) {
        this.reservations = reservations;
        this.notes = notes;
        this.clients = clients;
        this.currentUser = currentUser;
        this.parser = parser;
        this.mapper = mapper;
        this.priorityCalculator = priorityCalculator;
        this.audit = audit;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> search(String query, Pageable pageable) {
        ReservationFilter filter = parser.parse(query);
        return reservations.findAll(ReservationSpecifications.from(filter), pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ReservationResponse get(UUID id) {
        return mapper.toResponse(require(id));
    }

    @Transactional
    public ReservationResponse create(ReservationRequest request) {
        validateCode(request.code(), null);
        UserAccount actor = currentUser.requireEntity();
        Reservation reservation = new Reservation(UUID.randomUUID(), normalizeRequired(request.code()),
                normalizeRequired(request.animalName()), actor);
        apply(reservation, request, actor);
        reservations.save(reservation);
        audit.record("RESERVATION_CREATED", "RESERVATION", reservation.getId(),
                "Создана заявка " + reservation.getCode(), Map.of());
        return mapper.toResponse(reservation);
    }

    @Transactional
    public ReservationResponse update(UUID id, ReservationRequest request) {
        Reservation reservation = require(id);
        verifyVersion(request.version(), reservation.getVersion());
        validateCode(request.code(), id);
        UserAccount actor = currentUser.requireEntity();
        apply(reservation, request, actor);
        audit.record("RESERVATION_UPDATED", "RESERVATION", id,
                "Изменена заявка " + reservation.getCode(), Map.of("version", reservation.getVersion()));
        return mapper.toResponse(reservation);
    }

    @Transactional
    public ReservationResponse archive(UUID id) {
        Reservation reservation = require(id);
        archiveInternal(reservation, currentUser.requireEntity());
        audit.record("RESERVATION_ARCHIVED", "RESERVATION", id, "Заявка перемещена в архив", Map.of());
        return mapper.toResponse(reservation);
    }

    @Transactional
    public ReservationResponse restore(UUID id) {
        Reservation reservation = require(id);
        reservation.setArchived(false);
        reservation.setArchivedAt(null);
        reservation.setArchivedBy(null);
        reservation.setUpdatedBy(currentUser.requireEntity());
        audit.record("RESERVATION_RESTORED", "RESERVATION", id, "Заявка восстановлена из архива", Map.of());
        return mapper.toResponse(reservation);
    }

    @Transactional
    public void permanentDelete(UUID id, String confirmation) {
        if (!properties.permanentDeleteEnabled()) {
            throw new BadRequestException("Безвозвратное удаление отключено конфигурацией сервера");
        }
        if (!"PERMANENTLY_DELETE".equals(confirmation)) {
            throw new BadRequestException("Для удаления требуется заголовок X-Confirm-Delete: PERMANENTLY_DELETE");
        }
        Reservation reservation = require(id);
        String code = reservation.getCode();
        notes.deleteByReservationId(id);
        reservations.delete(reservation);
        audit.record("RESERVATION_PERMANENTLY_DELETED", "RESERVATION", id,
                "Безвозвратно удалена заявка " + code, Map.of());
    }

    @Transactional
    public BulkResult bulk(BulkRequest request) {
        validateBulkRequest(request);
        UserAccount actor = currentUser.requireEntity();
        int succeeded = 0;
        var failures = new ArrayList<BulkFailure>();
        for (UUID id : request.ids()) {
            Reservation reservation = reservations.findById(id).orElse(null);
            if (reservation == null) {
                failures.add(new BulkFailure(id, "Заявка не найдена"));
                continue;
            }
            try {
                applyBulk(reservation, request, actor);
                succeeded++;
            } catch (RuntimeException ex) {
                failures.add(new BulkFailure(id, ex.getMessage()));
            }
        }
        audit.record("RESERVATION_BULK_" + request.action().name(), "RESERVATION", null,
                "Выполнена массовая операция", Map.of("requested", request.ids().size(), "succeeded", succeeded));
        return new BulkResult(request.ids().size(), succeeded, failures);
    }

    Reservation upsertFromImport(Reservation existing, ReservationRequest request, UserAccount actor) {
        if (existing == null) {
            Reservation created = new Reservation(UUID.randomUUID(), normalizeRequired(request.code()),
                    normalizeRequired(request.animalName()), actor);
            apply(created, request, actor);
            return reservations.save(created);
        }
        apply(existing, request, actor);
        return existing;
    }

    private void apply(Reservation reservation, ReservationRequest request, UserAccount actor) {
        reservation.setCode(normalizeRequired(request.code()));
        reservation.setAnimalName(normalizeRequired(request.animalName()));
        reservation.setComment(normalizeNullable(request.comment()));
        reservation.setReservationDate(request.reservationDate());
        reservation.setClient(resolveClient(request.clientId()));
        reservation.setStatus(request.status());
        reservation.setDepositAmount(request.depositAmount());
        reservation.setNeedsCare(request.needsCare());
        reservation.setTags(normalizeTags(request.tags()));
        reservation.setPriority(priorityCalculator.calculate(request.status(), request.depositAmount(), request.needsCare()));
        reservation.setUpdatedBy(actor);
    }

    private void applyBulk(Reservation reservation, BulkRequest request, UserAccount actor) {
        switch (request.action()) {
            case ARCHIVE -> archiveInternal(reservation, actor);
            case RESTORE -> {
                reservation.setArchived(false);
                reservation.setArchivedAt(null);
                reservation.setArchivedBy(null);
                reservation.setUpdatedBy(actor);
            }
            case SET_STATUS -> {
                reservation.setStatus(request.status());
                reservation.setPriority(priorityCalculator.calculate(
                        reservation.getStatus(), reservation.getDepositAmount(), reservation.isNeedsCare()));
                reservation.setUpdatedBy(actor);
            }
            case ADD_TAG -> {
                Set<String> tags = new LinkedHashSet<>(reservation.getTags());
                tags.add(normalizeTag(request.tag()));
                reservation.setTags(tags);
                reservation.setUpdatedBy(actor);
            }
            case REMOVE_TAG -> {
                Set<String> tags = new LinkedHashSet<>(reservation.getTags());
                tags.remove(normalizeTag(request.tag()));
                reservation.setTags(tags);
                reservation.setUpdatedBy(actor);
            }
        }
    }

    private static void validateBulkRequest(BulkRequest request) {
        if (request.action() == BulkAction.SET_STATUS && request.status() == null) {
            throw new BadRequestException("Для SET_STATUS требуется status");
        }
        if ((request.action() == BulkAction.ADD_TAG || request.action() == BulkAction.REMOVE_TAG)
                && (request.tag() == null || request.tag().isBlank())) {
            throw new BadRequestException("Для операции с тегом требуется tag");
        }
    }

    private static void archiveInternal(Reservation reservation, UserAccount actor) {
        reservation.setArchived(true);
        reservation.setArchivedAt(Instant.now());
        reservation.setArchivedBy(actor);
        reservation.setUpdatedBy(actor);
    }

    private Reservation require(UUID id) {
        return reservations.findById(id).orElseThrow(() -> new NotFoundException("Заявка не найдена"));
    }

    private ShelterClient resolveClient(UUID id) {
        ShelterClient client = clients.findById(id).orElseThrow(() -> new BadRequestException("Клиент не найден"));
        if (!client.isActive()) throw new BadRequestException("Нельзя назначить неактивного клиента");
        return client;
    }

    private void validateCode(String code, UUID currentId) {
        String normalized = normalizeRequired(code);
        boolean exists = currentId == null
                ? reservations.existsByCodeIgnoreCase(normalized)
                : reservations.existsByCodeIgnoreCaseAndIdNot(normalized, currentId);
        if (exists) throw new ConflictException("Код заявки уже используется");
    }

    private static void verifyVersion(Long requested, long actual) {
        if (requested == null) throw new BadRequestException("Для изменения требуется поле version");
        if (requested != actual) throw new ConflictException("Заявка уже изменена. Обновите данные и повторите операцию");
    }

    private static Set<String> normalizeTags(Set<String> tags) {
        if (tags == null) return Set.of();
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String tag : tags) result.add(normalizeTag(tag));
        if (result.size() > 20) throw new BadRequestException("Допустимо не более 20 тегов");
        return result;
    }

    private static String normalizeTag(String value) {
        String normalized = normalizeRequired(value).toLowerCase(Locale.ROOT);
        if (normalized.length() > 50) throw new BadRequestException("Тег длиннее 50 символов");
        return normalized;
    }

    private static String normalizeRequired(String value) {
        if (value == null || value.trim().isEmpty()) throw new BadRequestException("Обязательное текстовое поле не заполнено");
        return value.trim().replaceAll("\\s+", " ");
    }

    private static String normalizeNullable(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim().replaceAll("\\s+", " ");
    }
}
