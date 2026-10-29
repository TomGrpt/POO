package fr.imt.arena.jeux;

// Sélection individuelle
public final class JeuSolo extends Jeu {
    public JeuSolo(String nom, int manches) {
        super(nom, manches);
    }

    @Override
    public int getJoueursParEquipe() {
        return 1;
    }
}
