package fr.imt.arena.jeux;

// Sélection collective
public final class JeuEquipe extends Jeu {
    private final int joueurs;

    public JeuEquipe(String nom, int manches, int joueurs) {
        super(nom, manches);
        if (joueurs < 3 || joueurs > 5) {
            throw new IllegalArgumentException("Entre 3 et 5 joueurs");
        }
        this.joueurs = joueurs;
    }

    @Override
    public int getJoueursParEquipe() {
        return joueurs;
    }
}
