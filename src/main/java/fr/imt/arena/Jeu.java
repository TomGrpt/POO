package fr.imt.arena;

import java.util.ArrayList;
import java.util.List;

public abstract class Jeu {
    private final String nom;
    private final int manches;

    protected Jeu(String nom, int manches) {
        this.nom = Verifications.texte(nom);
        if (manches < 1) {
            throw new IllegalArgumentException("Un match doit avoir au moins une manche");
        }
        this.manches = manches;
    }

    public String nom() {
        return nom;
    }

    public int manches() {
        return manches;
    }

    public abstract int joueursParEquipe();

    public List<Joueur> selectionner(List<Joueur> effectif) {
        int nombreJoueurs = joueursParEquipe();
        if (effectif.size() < nombreJoueurs) {
            throw new IllegalStateException("Pas assez de joueurs pour " + nom);
        }
        List<Joueur> selection = new ArrayList<>();
        for (int i = 0; i < nombreJoueurs; i++) {
            selection.add(effectif.get(i));
        }
        return List.copyOf(selection);
    }

    @Override
    public final boolean equals(Object objet) {
        if (!(objet instanceof Jeu)) {
            return false;
        }
        Jeu autre = (Jeu) objet;
        return nom.equals(autre.nom);
    }

    @Override
    public final int hashCode() {
        return nom.hashCode();
    }
}
