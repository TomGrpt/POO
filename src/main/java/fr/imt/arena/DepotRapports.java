package fr.imt.arena;

import java.util.List;

public interface DepotRapports extends AutoCloseable {
    void sauvegarder(Rapport rapport);
    List<Rapport> tous();
    List<Rapport> parJeu(String jeu);

    @Override
    default void close() {
        // Un dépôt en mémoire n'a pas de connexion à fermer.
    }
}
