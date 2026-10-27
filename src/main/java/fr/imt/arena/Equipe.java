package fr.imt.arena;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Equipe implements Classe {
    private final String nom;
    private final List<Joueur> joueurs = new ArrayList<>();
    private Coach coach;
    private int points;
    private Object reservation;

    public Equipe(String nom) {
        this.nom = Verifications.texte(nom);
    }

    @Override
    public String nom() {
        return nom;
    }

    @Override
    public synchronized int points() {
        return points;
    }

    public synchronized List<Joueur> joueurs() {
        return List.copyOf(joueurs);
    }

    public synchronized Optional<Coach> coach() {
        return Optional.ofNullable(coach);
    }

    public synchronized boolean engagee() {
        return reservation != null;
    }

    private void verifierModificationPossible() {
        if (engagee()) {
            throw new IllegalStateException("L'équipe joue déjà dans un tournoi");
        }
    }

    public synchronized void ajouter(Joueur joueur) {
        verifierModificationPossible();
        joueur.rejoindre(this);
        joueurs.add(joueur);
    }

    public synchronized void retirer(Joueur joueur) {
        verifierModificationPossible();
        int position = joueurs.indexOf(joueur);
        if (position == -1) {
            throw new IllegalArgumentException("Le joueur n'est pas dans l'équipe");
        }
        Joueur joueurInscrit = joueurs.get(position);
        joueurInscrit.quitter(this);
        joueurs.remove(position);
    }

    public synchronized void nommer(Coach nouveauCoach) {
        verifierModificationPossible();
        if (coach == nouveauCoach) {
            return;
        }
        // Vérifier le nouveau avant de détacher l'ancien.
        if (nouveauCoach != null) {
            nouveauCoach.rejoindre(this);
        }
        if (coach != null) {
            coach.quitter(this);
        }
        coach = nouveauCoach;
    }

    synchronized void verifier(Jeu jeu) {
        if (coach == null) {
            throw new IllegalStateException("Coach manquant : " + nom);
        }
        jeu.selectionner(joueurs);
    }

    synchronized void reserver(Object tournoi, Jeu jeu) {
        verifierModificationPossible();
        verifier(jeu);
        reservation = tournoi;
    }

    synchronized void liberer(Object tournoi) {
        if (reservation != tournoi) {
            throw new IllegalStateException("Ce tournoi n'a pas réservé cette équipe");
        }
        reservation = null;
    }

    synchronized void victoire() {
        points = points + 3;
        if (coach != null) {
            coach.victoire();
        }
        for (Joueur joueur : joueurs) {
            joueur.victoire();
        }
    }

    @Override
    public String toString() {
        return nom;
    }
}
