package fr.imt.arena;

import java.util.Optional;

public final class Joueur extends Personne {
    private Equipe equipe;
    private StatutJoueur statut;
    private int victoiresDepuisArrivee;

    public Joueur(String id, String nom, String prenom, String pseudo,
                  Genre genre, String telephone, StatutJoueur statut) {
        super(id, nom, prenom, pseudo, genre, telephone);
        if (statut == null) {
            throw new IllegalArgumentException("Le statut doit être renseigné");
        }
        this.statut = statut;
    }

    public synchronized Optional<Equipe> equipe() {
        return Optional.ofNullable(equipe);
    }

    public synchronized StatutJoueur statut() {
        return statut;
    }

    public synchronized int victoiresDepuisArrivee() {
        return victoiresDepuisArrivee;
    }

    // Ces méthodes sont appelées par Equipe, pas directement depuis une autre application.
    synchronized void rejoindre(Equipe nouvelleEquipe) {
        if (equipe != null) {
            throw new IllegalStateException("Le joueur appartient déjà à une équipe");
        }
        equipe = nouvelleEquipe;
        victoiresDepuisArrivee = 0;
    }

    synchronized void quitter(Equipe ancienneEquipe) {
        if (equipe != ancienneEquipe) {
            throw new IllegalStateException("Le joueur n'appartient pas à cette équipe");
        }
        equipe = null;
        victoiresDepuisArrivee = 0;
    }

    synchronized void victoire() {
        victoiresDepuisArrivee++;
        if (victoiresDepuisArrivee >= 3) {
            statut = StatutJoueur.PROFESSIONNEL;
        }
    }
}
