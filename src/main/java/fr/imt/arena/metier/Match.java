package fr.imt.arena.metier;

import fr.imt.arena.jeux.Jeu;

import java.util.List;
import java.util.Objects;

// Résultat immuable
public final class Match {
    private final Equipe equipe1;
    private final Equipe equipe2;
    private final Jeu jeu;
    private final Arbitre arbitre;
    private final Equipe vainqueur;
    private final List<Joueur> joueurs1;
    private final List<Joueur> joueurs2;

    Match(Equipe a, Equipe b, Jeu jeu, Arbitre arbitre, Equipe vainqueur) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        if (a == b) {
            throw new IllegalArgumentException("Deux équipes différentes requises");
        }
        if (!arbitre.estQualifie(jeu)) {
            throw new IllegalArgumentException("Arbitre non qualifié");
        }
        if (vainqueur != null && vainqueur != a && vainqueur != b) {
            throw new IllegalArgumentException("Vainqueur extérieur au match");
        }
        this.equipe1 = a;
        this.equipe2 = b;
        this.jeu = jeu;
        this.arbitre = arbitre;
        this.vainqueur = vainqueur;
        this.joueurs1 = jeu.selectionner(a);
        this.joueurs2 = jeu.selectionner(b);
    }

    public Equipe getEquipe1() {
        return equipe1;
    }

    public Equipe getEquipe2() {
        return equipe2;
    }

    public Jeu getJeu() {
        return jeu;
    }

    public Arbitre getArbitre() {
        return arbitre;
    }

    public Equipe getVainqueur() {
        return vainqueur;
    }

    public boolean estJoue() {
        return vainqueur != null;
    }

    public List<Joueur> getJoueurs1() {
        return joueurs1;
    }

    public List<Joueur> getJoueurs2() {
        return joueurs2;
    }

    @Override
    public String toString() {
        String resultat = "annulé";
        if (estJoue()) {
            resultat = vainqueur.getNom();
        }
        return equipe1 + " / " + equipe2 + " : " + resultat;
    }
}
