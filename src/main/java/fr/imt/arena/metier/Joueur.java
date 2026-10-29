package fr.imt.arena.metier;

// Statut du joueur
public final class Joueur extends Personne {
    private Equipe equipe;
    private StatutJoueur statut = StatutJoueur.AMATEUR;
    private int victoiresDepuisArrivee;

    public Joueur(int id, String nom, String prenom, String pseudo, Genre genre, String tel) {
        super(id, nom, prenom, pseudo, genre, tel);
    }

    public Equipe getEquipe() {
        synchronized (Verrou.OBJET) {
            return equipe;
        }
    }

    public StatutJoueur getStatut() {
        synchronized (Verrou.OBJET) {
            return statut;
        }
    }

    public int getVictoiresDepuisArrivee() {
        synchronized (Verrou.OBJET) {
            return victoiresDepuisArrivee;
        }
    }

    // Changement d’équipe
    void affecter(Equipe nouvelle) {
        equipe = nouvelle;
        victoiresDepuisArrivee = 0;
    }

    // Progression du statut
    void gagner() {
        victoiresDepuisArrivee++;
        if (victoiresDepuisArrivee >= 3) {
            statut = StatutJoueur.PRO;
        }
    }
}
