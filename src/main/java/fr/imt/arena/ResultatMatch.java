package fr.imt.arena;

/** Copie des informations d'un match, sans référence vers un objet modifiable. */
public final class ResultatMatch {
    private final String premiere;
    private final String seconde;
    private final EtatMatch etat;
    private final String vainqueur;

    public ResultatMatch(String premiere, String seconde, EtatMatch etat, String vainqueur) {
        this.premiere = premiere;
        this.seconde = seconde;
        this.etat = etat;
        this.vainqueur = vainqueur;
    }

    public String premiere() {
        return premiere;
    }

    public String seconde() {
        return seconde;
    }

    public EtatMatch etat() {
        return etat;
    }

    public String vainqueur() {
        return vainqueur;
    }
}
