package fr.imt.arena.metier;

import fr.imt.arena.jeux.Jeu;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

// Qualifications et disponibilité
public final class Arbitre extends Personne {
    private final Set<Jeu> qualifications = new HashSet<>();
    private int matchsArbitres;
    private boolean occupe;

    public Arbitre(
            int id,
            String nom,
            String prenom,
            String pseudo,
            Genre genre,
            String tel,
            Jeu premierJeu) {
        super(id, nom, prenom, pseudo, genre, tel);
        qualifications.add(Objects.requireNonNull(premierJeu));
    }

    // Ajout d’une qualification
    public void qualifier(Jeu jeu) {
        synchronized (Verrou.OBJET) {
            if (occupe) {
                throw new IllegalStateException("Arbitre occupé");
            }
            qualifications.add(Objects.requireNonNull(jeu));
        }
    }

    public Set<Jeu> getQualifications() {
        synchronized (Verrou.OBJET) {
            return Set.copyOf(qualifications);
        }
    }

    public boolean estQualifie(Jeu jeu) {
        synchronized (Verrou.OBJET) {
            return qualifications.contains(jeu);
        }
    }

    public boolean estOccupe() {
        synchronized (Verrou.OBJET) {
            return occupe;
        }
    }

    public int getMatchsArbitres() {
        synchronized (Verrou.OBJET) {
            return matchsArbitres;
        }
    }

    // Réservation de l’arbitre
    void reserver(boolean valeur) {
        occupe = valeur;
    }

    void compterMatch() {
        matchsArbitres++;
    }
}
