package petstore.fx;

import petstore.model.Client;
import petstore.repository.EntityRepository;
import petstore.repository.JdbcClientRepository;
import petstore.repository.JdbcReservationRepository;
import petstore.repository.ReservationRepository;
import petstore.service.ClientService;
import petstore.service.ExportService;
import petstore.service.ReservationService;
import petstore.service.StatisticsService;

public final class AppContext {
    private final ClientService clients;
    private final ReservationService reservations;
    private final StatisticsService statistics;
    private final ExportService export;

    public AppContext() {
        EntityRepository<Client> clientRepository = new JdbcClientRepository();
        ReservationRepository reservationRepository = new JdbcReservationRepository();
        this.clients = new ClientService(clientRepository, reservationRepository);
        this.reservations = new ReservationService(clientRepository, reservationRepository);
        this.statistics = new StatisticsService(clientRepository, reservationRepository);
        this.export = new ExportService();
    }

    public ClientService clients() {
        return clients;
    }

    public ReservationService reservations() {
        return reservations;
    }

    public StatisticsService statistics() {
        return statistics;
    }

    public ExportService export() {
        return export;
    }
}
