package fr.imt.arena;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Equipe implements Classe {
    private final String nom;
    private final List<Joueur> joueurs = new ArrayList<>();
    private Coach coach;
    private int points;
    private Object reservation;
    public Equipe(String nom) { this.nom = Verifications.texte(nom); }
    @Override public String nom() { return nom; }
    @Override public synchronized int points() { return points; }
    public synchronized List<Joueur> joueurs() { return List.copyOf(joueurs); }
    public synchronized Optional<Coach> coach() { return Optional.ofNullable(coach); }
    public synchronized boolean engagee() { return reservation != null; }
    private void modifiable() { if (engagee()) throw new IllegalStateException("Equipe engagée dans un tournoi"); }
    public synchronized void ajouter(Joueur joueur) {
        modifiable(); joueur.rejoindre(this); joueurs.add(joueur);
    }
    public synchronized void retirer(Joueur joueur) {
        modifiable();
        if (!joueurs.contains(joueur)) throw new IllegalArgumentException("Joueur absent");
        // Utiliser l'instance réellement inscrite, pas une éventuelle copie d'identité.
        Joueur inscrit = joueurs.get(joueurs.indexOf(joueur)); inscrit.quitter(this); joueurs.remove(inscrit);
    }
    public synchronized void nommer(Coach nouveau) {
        modifiable();
        if (coach == nouveau) return;
        if (nouveau != null) nouveau.rejoindre(this);
        if (coach != null) coach.quitter(this);
        coach = nouveau;
    }
    synchronized void verifier(Jeu jeu) {
        if (coach == null) throw new IllegalStateException("Coach manquant : " + nom);
        jeu.selectionner(joueurs);
    }
    synchronized void reserver(Object proprietaire, Jeu jeu) {
        modifiable(); verifier(jeu); reservation = proprietaire;
    }
    synchronized void liberer(Object proprietaire) {
        if (reservation != proprietaire) throw new IllegalStateException("Réservation étrangère");
        reservation = null;
    }
    synchronized void victoire() {
        points += 3;
        if (coach != null) coach.victoire();
        joueurs.forEach(Joueur::victoire);
    }
    @Override public String toString() { return nom; }
}
