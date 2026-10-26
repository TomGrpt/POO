package fr.imt.arena;

import jakarta.persistence.*;
import java.util.List;
import java.util.Map;

/** Une factory partagée ; un EntityManager local par opération, jamais partagé entre threads. */
public final class DepotJpa implements DepotRapports {
    private final EntityManagerFactory factory;
    public DepotJpa() { this("jdbc:h2:file:./data/arena"); }
    public DepotJpa(String url) {
        factory = Persistence.createEntityManagerFactory("arena", Map.of("jakarta.persistence.jdbc.url", url));
    }
    @Override public void sauvegarder(Rapport rapport) {
        try (EntityManager em = factory.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try { transaction.begin(); em.persist(rapport); transaction.commit(); }
            catch (RuntimeException e) { if (transaction.isActive()) transaction.rollback(); throw e; }
        }
    }
    @Override public List<Rapport> tous() {
        try (EntityManager em = factory.createEntityManager()) {
            return List.copyOf(em.createQuery("select r from Rapport r order by r.date, r.id", Rapport.class).getResultList());
        }
    }
    @Override public List<Rapport> parJeu(String jeu) {
        try (EntityManager em = factory.createEntityManager()) {
            return List.copyOf(em.createQuery("select r from Rapport r where r.jeu = :jeu order by r.date, r.id", Rapport.class)
                .setParameter("jeu", jeu).getResultList());
        }
    }
    @Override public void close() { factory.close(); }
}
