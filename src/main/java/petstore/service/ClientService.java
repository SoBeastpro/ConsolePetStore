package petstore.service;

import petstore.exception.BusinessException;
import petstore.exception.EntityNotFoundException;
import petstore.exception.Messages;
import petstore.model.Client;
import petstore.repository.EntityRepository;
import petstore.repository.ReservationRepository;
import org.postgresql.util.PSQLException;

import java.sql.SQLException;
import java.text.Collator;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

    public List<Client> list() throws SQLException {
        Collator collator = Collator.getInstance(Locale.forLanguageTag("ru"));
        collator.setStrength(Collator.PRIMARY);
        return clientRepository.findAll().stream()
                .sorted(Comparator.comparing(Client::getFullName, Comparator.nullsFirst(collator))
                        .thenComparingLong(Client::getId))
                .toList();
    }

    public Client getById(long id) throws SQLException {
        return clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Messages.clientNotFound(id)));
    }

    public long create(String fullName, String phone, String email) throws SQLException {
        Client client = new Client(normalize(fullName), normalize(phone), normalize(email));
        validate(client);
        ensureUnique(client);
        try {
            return clientRepository.insert(client);
        } catch (SQLException exception) {
            throw uniqueOrOriginal(exception);
        }
    }

    public void update(long id, String fullName, String phone, String email) throws SQLException {
        Client client = getById(id);
        client.setFullName(normalize(fullName));
        client.setPhone(normalize(phone));
        client.setEmail(normalize(email));
        validate(client);
        ensureUnique(client);
        try {
            clientRepository.update(client);
        } catch (SQLException exception) {
            throw uniqueOrOriginal(exception);
        }
    }

    public void delete(long id) throws SQLException {
        getById(id);
        if (reservationRepository.countByClientId(id) > 0) {
            throw new BusinessException(Messages.CLIENT_HAS_RESERVATIONS);
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
        if (client.getFullName().isBlank()) {
            throw new BusinessException(Messages.CLIENT_NAME_REQUIRED);
        }
        if (client.getFullName().length() > 100) {
            throw new BusinessException(Messages.CLIENT_NAME_LENGTH);
        }
        if (client.getPhone().isBlank()) {
            throw new BusinessException(Messages.CLIENT_PHONE_REQUIRED);
        }
        if (client.getPhone().length() > 20) {
            throw new BusinessException(Messages.CLIENT_PHONE_LENGTH);
        }
        if (client.getEmail().isBlank()) {
            throw new BusinessException(Messages.CLIENT_EMAIL_REQUIRED);
        }
        if (client.getEmail().length() > 100) {
            throw new BusinessException(Messages.CLIENT_EMAIL_LENGTH);
        }
    }

    private void ensureUnique(Client client) throws SQLException {
        for (Client other : clientRepository.findAll()) {
            if (other.getId() == client.getId()) {
                continue;
            }
            if (client.getPhone().equals(other.getPhone())) {
                throw new BusinessException(Messages.CLIENT_PHONE_DUPLICATE);
            }
            if (client.getEmail().equals(other.getEmail())) {
                throw new BusinessException(Messages.CLIENT_EMAIL_DUPLICATE);
            }
        }
    }

    private RuntimeException uniqueOrOriginal(SQLException exception) throws SQLException {
        if (!"23505".equals(exception.getSQLState())) {
            throw exception;
        }
        String constraint = constraintName(exception);
        if ("clients_phone_key".equals(constraint)) {
            return new BusinessException(Messages.CLIENT_PHONE_DUPLICATE);
        }
        if ("clients_email_key".equals(constraint)) {
            return new BusinessException(Messages.CLIENT_EMAIL_DUPLICATE);
        }
        return new BusinessException(Messages.CLIENT_DUPLICATE);
    }

    private String constraintName(SQLException exception) {
        if (exception instanceof PSQLException psql && psql.getServerErrorMessage() != null) {
            return psql.getServerErrorMessage().getConstraint();
        }
        String message = exception.getMessage();
        if (message == null) {
            return null;
        }
        if (message.contains("clients_phone_key")) {
            return "clients_phone_key";
        }
        if (message.contains("clients_email_key")) {
            return "clients_email_key";
        }
        return null;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
