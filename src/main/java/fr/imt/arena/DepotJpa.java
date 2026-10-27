package fr.imt.arena;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import jakarta.persistence.TypedQuery;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DepotJpa implements DepotRapports {
    private final EntityManagerFactory factory;

    public DepotJpa() {
        this("jdbc:h2:file:./data/arena");
    }

    public DepotJpa(String url) {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("jakarta.persistence.jdbc.url", url);
        factory = Persistence.createEntityManagerFactory("arena", configuration);
    }

    @Override
    public void sauvegarder(Rapport rapport) {
        // Chaque appel crée son propre EntityManager, y compris depuis les workers.
        try (EntityManager manager = factory.createEntityManager()) {
            EntityTransaction transaction = manager.getTransaction();
            try {
                transaction.begin();
                manager.persist(rapport);
                transaction.commit();
            } catch (RuntimeException erreur) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw erreur;
            }
        }
    }

    @Override
    public List<Rapport> tous() {
        try (EntityManager manager = factory.createEntityManager()) {
            String texte = "select r from Rapport r order by r.date, r.id";
            TypedQuery<Rapport> requete = manager.createQuery(texte, Rapport.class);
            return List.copyOf(requete.getResultList());
        }
    }

    @Override
    public List<Rapport> parJeu(String jeu) {
        try (EntityManager manager = factory.createEntityManager()) {
            String texte = "select r from Rapport r where r.jeu = :jeu order by r.date, r.id";
            TypedQuery<Rapport> requete = manager.createQuery(texte, Rapport.class);
            requete.setParameter("jeu", jeu);
            return List.copyOf(requete.getResultList());
        }
    }

    @Override
    public void close() {
        factory.close();
    }
}
