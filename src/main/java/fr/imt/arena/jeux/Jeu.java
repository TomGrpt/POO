package fr.imt.arena.jeux;

import fr.imt.arena.metier.Equipe;
import fr.imt.arena.metier.Joueur;
import fr.imt.arena.util.Textes;

import java.util.ArrayList;
import java.util.List;

// Règles communes
public abstract class Jeu {
    private final String nom;
    private final int manches;

    protected Jeu(String nom, int manches) {
        if (manches < 1) {
            throw new IllegalArgumentException("Au moins une manche");
        }
        this.nom = Textes.verifier(nom);
        this.manches = manches;
    }

    public String getNom() {
        return nom;
    }

    public int getManches() {
        return manches;
    }

    public abstract int getJoueursParEquipe();

    // Choix des titulaires
    public List<Joueur> selectionner(Equipe equipe) {
        List<Joueur> joueurs = equipe.getJoueurs();
        int nombre = getJoueursParEquipe();
        if (nombre < 1 || joueurs.size() < nombre) {
            throw new IllegalStateException("Pas assez de joueurs pour " + nom);
        }
        List<Joueur> selection = new ArrayList<>();
        for (int i = 0; i < nombre; i++) {
            selection.add(joueurs.get(i));
        }
        return List.copyOf(selection);
    }

    @Override
    public String toString() {
        return nom;
    }
}
