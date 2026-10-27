package fr.imt.arena;

import java.util.Optional;

public final class Coach extends Personne {
    private Equipe equipe;
    private int victoires;

    public Coach(String id, String nom, String prenom, String pseudo,
                 Genre genre, String telephone) {
        super(id, nom, prenom, pseudo, genre, telephone);
    }

    public synchronized Optional<Equipe> equipe() {
        return Optional.ofNullable(equipe);
    }

    public synchronized int victoires() {
        return victoires;
    }

    synchronized void rejoindre(Equipe nouvelleEquipe) {
        if (equipe != null) {
            throw new IllegalStateException("Le coach dirige déjà une équipe");
        }
        equipe = nouvelleEquipe;
    }

    synchronized void quitter(Equipe ancienneEquipe) {
        if (equipe != ancienneEquipe) {
            throw new IllegalStateException("Le coach ne dirige pas cette équipe");
        }
        equipe = null;
        // Les victoires restent acquises pendant toute la carrière.
    }

    synchronized void victoire() {
        victoires++;
    }
}
