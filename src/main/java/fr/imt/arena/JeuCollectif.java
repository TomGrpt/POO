package fr.imt.arena;

public final class JeuCollectif extends Jeu {
    private final int nombreJoueurs;

    public JeuCollectif(String nom, int manches, int nombreJoueurs) {
        super(nom, manches);
        if (nombreJoueurs < 3 || nombreJoueurs > 5) {
            throw new IllegalArgumentException("Il faut entre 3 et 5 joueurs");
        }
        this.nombreJoueurs = nombreJoueurs;
    }

    @Override
    public int joueursParEquipe() {
        return nombreJoueurs;
    }
}
