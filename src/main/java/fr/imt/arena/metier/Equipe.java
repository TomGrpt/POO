package fr.imt.arena.metier;

import fr.imt.arena.util.Textes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Composition de l’équipe
public final class Equipe implements Classable {
    private final String nom;
    private final List<Joueur> joueurs = new ArrayList<>();
    private Coach coach;
    private int points;
    private boolean occupee;

    public Equipe(String nom) {
        this.nom = Textes.verifier(nom);
    }

    public String getNom() {
        return nom;
    }

    public Coach getCoach() {
        synchronized (Verrou.OBJET) {
            return coach;
        }
    }

    public List<Joueur> getJoueurs() {
        synchronized (Verrou.OBJET) {
            return List.copyOf(joueurs);
        }
    }

    @Override
    public int getPoints() {
        synchronized (Verrou.OBJET) {
            return points;
        }
    }

    public boolean estOccupee() {
        synchronized (Verrou.OBJET) {
            return occupee;
        }
    }

    private void verifierLibre() {
        if (occupee) {
            throw new IllegalStateException("Equipe en tournoi : " + nom);
        }
    }

    // Transfert du joueur
    public void ajouterJoueur(Joueur joueur) {
        synchronized (Verrou.OBJET) {
            verifierLibre();
            Objects.requireNonNull(joueur);
            Equipe ancienne = joueur.getEquipe();
            if (ancienne == this) {
                return;
            }
            if (ancienne != null) {
                ancienne.verifierLibre();
                ancienne.joueurs.remove(joueur);
            }
            joueurs.add(joueur);
            joueur.affecter(this);
        }
    }

    // Retrait du joueur
    public void retirerJoueur(Joueur joueur) {
        synchronized (Verrou.OBJET) {
            verifierLibre();
            if (joueurs.remove(joueur)) {
                joueur.affecter(null);
            }
        }
    }

    // Transfert du coach
    public void definirCoach(Coach nouveau) {
        synchronized (Verrou.OBJET) {
            verifierLibre();
            Objects.requireNonNull(nouveau);
            Equipe ancienne = nouveau.getEquipe();
            if (ancienne == this) {
                return;
            }
            if (ancienne != null) {
                ancienne.verifierLibre();
                ancienne.coach = null;
            }
            if (coach != null) {
                coach.affecter(null);
            }
            coach = nouveau;
            nouveau.affecter(this);
        }
    }

    void reserver(boolean valeur) {
        occupee = valeur;
    }

    // Mise à jour collective
    void gagner() {
        points += 3;
        coach.gagner();

        for (Joueur joueur : joueurs) {
            joueur.gagner();
        }
    }

    @Override
    public String toString() {
        return nom;
    }
}
