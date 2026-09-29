package ru.mirea.project.service;

import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.Client;
import ru.mirea.project.repository.EntityRepository;
import ru.mirea.project.repository.ReservationRepository;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientService {
    private final EntityRepository<Client> clientRepository;
    private final ReservationRepository reservationRepository;

    public ClientService(EntityRepository<Client> clientRepository, ReservationRepository reservationRepository) {
        this.clientRepository = clientRepository;
        this.reservationRepository = reservationRepository;
    }

    public List<Client> getAll() throws SQLException {
        return clientRepository.findAll();
    }

    public Client getById(long id) throws SQLException {
        return clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Клиент с ID " + id + " не найден."));
    }

    public long create(String fullName, String phone, String email) throws SQLException {
        Client client = new Client(normalize(fullName), normalize(phone), normalize(email));
        validate(client);
        try {
            return clientRepository.insert(client);
        } catch (SQLException exception) {
            rethrowUnique(exception);
            throw exception;
        }
    }

    public void update(long id, String fullName, String phone, String email) throws SQLException {
        Client client = getById(id);
        client.setFullName(normalize(fullName));
        client.setPhone(normalize(phone));
        client.setEmail(normalize(email));
        validate(client);
        try {
            clientRepository.update(client);
        } catch (SQLException exception) {
            rethrowUnique(exception);
        }
    }

    public void delete(long id) throws SQLException {
        getById(id);
        if (reservationRepository.countByClientId(id) > 0) {
            throw new BusinessException("Нельзя удалить клиента, у которого есть заявки.");
        }
        clientRepository.delete(id);
    }

    public long count() throws SQLException {
        return clientRepository.findAll().size();
    }

    public Map<Long, String> namesById() throws SQLException {
        Map<Long, String> names = new HashMap<>();
        for (Client client : getAll()) {
            names.put(client.getId(), client.getFullName());
        }
        return names;
    }

    private void validate(Client client) {
        if (client.getFullName().isBlank() || client.getPhone().isBlank() || client.getEmail().isBlank()) {
            throw new BusinessException("Нельзя сохранить клиента без обязательных полей: ФИО, телефон, email.");
        }
        if (client.getFullName().length() > 100 || client.getPhone().length() > 20 || client.getEmail().length() > 100) {
            throw new BusinessException("Нельзя сохранить клиента: некорректная длина поля.");
        }
    }

    private void rethrowUnique(SQLException exception) throws SQLException {
        if ("23505".equals(exception.getSQLState())) {
            throw new BusinessException("Клиент с таким телефоном или email уже существует.");
        }
        throw exception;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
