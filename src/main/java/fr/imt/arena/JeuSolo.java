package fr.imt.arena;

public final class JeuSolo extends Jeu {
    public JeuSolo(String nom, int manches) { super(nom, manches); }
    @Override public int joueursParEquipe() { return 1; }
}
