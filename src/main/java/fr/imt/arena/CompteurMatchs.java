package fr.imt.arena;

/** Exemple d'observateur : compter les matchs joués ou annulés. */
public final class CompteurMatchs implements EcouteurMatch {
    private int nombre;

    @Override
    public synchronized void matchTermine(Match match) {
        nombre++;
    }

    public synchronized int nombre() {
        return nombre;
    }
}
