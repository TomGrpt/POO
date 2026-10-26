package fr.imt.arena;

import java.util.Objects;
import java.util.Optional;

public final class Joueur extends Personne {
    private Equipe equipe;
    private StatutJoueur statut;
    private int victoiresDepuisArrivee;
    public Joueur(String id, String nom, String prenom, String pseudo, Genre genre, String telephone, StatutJoueur statut) {
        super(id, nom, prenom, pseudo, genre, telephone); this.statut = Objects.requireNonNull(statut);
    }
    public synchronized Optional<Equipe> equipe() { return Optional.ofNullable(equipe); }
    public synchronized StatutJoueur statut() { return statut; }
    public synchronized int victoiresDepuisArrivee() { return victoiresDepuisArrivee; }
    synchronized void rejoindre(Equipe nouvelle) {
        if (equipe != null) throw new IllegalStateException("Joueur déjà dans une équipe");
        equipe = nouvelle; victoiresDepuisArrivee = 0;
    }
    synchronized void quitter(Equipe ancienne) {
        if (equipe != ancienne) throw new IllegalStateException("Mauvaise équipe");
        equipe = null; victoiresDepuisArrivee = 0;
    }
    synchronized void victoire() {
        if (++victoiresDepuisArrivee >= 3) statut = StatutJoueur.PROFESSIONNEL;
    }
}
