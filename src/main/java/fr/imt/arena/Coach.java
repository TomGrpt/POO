package fr.imt.arena;

import java.util.Optional;

public final class Coach extends Personne {
    private Equipe equipe;
    private int victoires;
    public Coach(String id, String nom, String prenom, String pseudo, Genre genre, String telephone) {
        super(id, nom, prenom, pseudo, genre, telephone);
    }
    public synchronized Optional<Equipe> equipe() { return Optional.ofNullable(equipe); }
    public synchronized int victoires() { return victoires; }
    synchronized void rejoindre(Equipe nouvelle) {
        if (equipe != null) throw new IllegalStateException("Coach déjà engagé");
        equipe = nouvelle;
    }
    synchronized void quitter(Equipe ancienne) {
        if (equipe != ancienne) throw new IllegalStateException("Mauvaise équipe");
        equipe = null;
    }
    synchronized void victoire() { victoires++; }
}
