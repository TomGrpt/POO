package fr.imt.arena;

import java.util.List;

public abstract class Jeu {
    private final String nom;
    private final int manches;
    protected Jeu(String nom, int manches) {
        this.nom = Verifications.texte(nom);
        if (manches < 1) throw new IllegalArgumentException("Au moins une manche");
        this.manches = manches;
    }
    public final String nom() { return nom; }
    public final int manches() { return manches; }
    public abstract int joueursParEquipe();
    public List<Joueur> selectionner(List<Joueur> effectif) {
        if (effectif.size() < joueursParEquipe()) throw new IllegalStateException("Effectif insuffisant pour " + nom);
        return List.copyOf(effectif.subList(0, joueursParEquipe()));
    }
    @Override public final boolean equals(Object o) { return o instanceof Jeu j && nom.equals(j.nom); }
    @Override public final int hashCode() { return nom.hashCode(); }
}
