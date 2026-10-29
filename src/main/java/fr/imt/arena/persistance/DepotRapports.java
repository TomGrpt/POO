package fr.imt.arena.persistance;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import jakarta.persistence.TypedQuery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Stockage des rapports
public final class DepotRapports implements AutoCloseable {
    private final EntityManagerFactory fabrique;

    public DepotRapports() {
        this("jdbc:h2:./data/arena");
    }

    public DepotRapports(String url) {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("jakarta.persistence.jdbc.url", url);
        fabrique = Persistence.createEntityManagerFactory("arena", configuration);
    }

    // Transaction indépendante
    public void enregistrer(RapportTournoi rapport) {
        try (EntityManager manager = fabrique.createEntityManager()) {
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

    public List<RapportTournoi> tous() {
        try (EntityManager manager = fabrique.createEntityManager()) {
            String texte = "select r from RapportTournoi r order by r.id";
            TypedQuery<RapportTournoi> requete = manager.createQuery(texte, RapportTournoi.class);
            return requete.getResultList();
        }
    }

    // Recherche par jeu
    public List<RapportTournoi> parJeu(String jeu) {
        try (EntityManager manager = fabrique.createEntityManager()) {
            String texte = "select r from RapportTournoi r where r.jeu = :jeu order by r.id";
            TypedQuery<RapportTournoi> requete = manager.createQuery(texte, RapportTournoi.class);
            requete.setParameter("jeu", jeu);
            return requete.getResultList();
        }
    }

    @Override
    public void close() {
        fabrique.close();
    }
}
