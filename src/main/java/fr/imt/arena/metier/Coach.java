package fr.imt.arena.metier;

// Carrière du coach
public final class Coach extends Personne {
    private Equipe equipe;
    private int victoires;

    public Coach(int id, String nom, String prenom, String pseudo, Genre genre, String tel) {
        super(id, nom, prenom, pseudo, genre, tel);
    }

    public Equipe getEquipe() {
        synchronized (Verrou.OBJET) {
            return equipe;
        }
    }

    public int getVictoires() {
        synchronized (Verrou.OBJET) {
            return victoires;
        }
    }

    // Changement d’équipe
    void affecter(Equipe equipe) {
        this.equipe = equipe;
    }

    // Victoires cumulées
    void gagner() {
        victoires++;
    }
}
