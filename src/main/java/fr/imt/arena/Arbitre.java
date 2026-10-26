package fr.imt.arena;

import java.util.Set;

public final class Arbitre extends Personne implements Qualifie {
    private final Set<Jeu> qualifications;
    private int matchsArbitres, engagements;
    public Arbitre(String id, String nom, String prenom, String pseudo, Genre genre, String telephone, Set<Jeu> qualifications) {
        super(id, nom, prenom, pseudo, genre, telephone);
        this.qualifications = Set.copyOf(qualifications);
        if (this.qualifications.isEmpty()) throw new IllegalArgumentException("Qualification obligatoire");
    }
    public Set<Jeu> qualifications() { return qualifications; }
    @Override public boolean estQualifie(Jeu jeu) { return qualifications.contains(jeu); }
    public synchronized int matchsArbitres() { return matchsArbitres; }
    public synchronized boolean disponible() { return engagements == 0; }
    synchronized void engager() { engagements++; }
    synchronized void liberer() { if (engagements <= 0) throw new IllegalStateException(); engagements--; }
    synchronized void matchJoue() { matchsArbitres++; }
}
