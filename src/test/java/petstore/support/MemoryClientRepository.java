package petstore.support;

import petstore.exception.EntityNotFoundException;
import petstore.exception.Messages;
import petstore.model.Client;
import petstore.repository.EntityRepository;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MemoryClientRepository implements EntityRepository<Client> {
    private final Map<Long, Client> rows = new LinkedHashMap<>();
    private long sequence = 1;

    @Override
    public List<Client> findAll() {
        return rows.values().stream()
                .sorted(Comparator.comparingLong(Client::getId))
                .map(this::copy)
                .toList();
    }

    @Override
    public Optional<Client> findById(long id) {
        Client client = rows.get(id);
        return client == null ? Optional.empty() : Optional.of(copy(client));
    }

    @Override
    public long insert(Client client) throws SQLException {
        for (Client other : rows.values()) {
            if (other.getPhone().equals(client.getPhone())) {
                throw new SQLException("duplicate key value violates unique constraint \"clients_phone_key\"", "23505");
            }
            if (other.getEmail().equals(client.getEmail())) {
                throw new SQLException("duplicate key value violates unique constraint \"clients_email_key\"", "23505");
            }
        }
        long id = sequence++;
        client.setId(id);
        rows.put(id, copy(client));
        return id;
    }

    @Override
    public void update(Client client) {
        if (!rows.containsKey(client.getId())) {
            throw new EntityNotFoundException(Messages.clientNotFound(client.getId()));
        }
        rows.put(client.getId(), copy(client));
    }

    @Override
    public void delete(long id) {
        if (rows.remove(id) == null) {
            throw new EntityNotFoundException(Messages.clientNotFound(id));
        }
    }

    private Client copy(Client client) {
        return new Client(client.getId(), client.getFullName(), client.getPhone(), client.getEmail());
    }
}
