package fr.imt.arena;

public abstract class Personne {
    private final String id;
    private final String nom;
    private final String prenom;
    private final String pseudo;
    private final Genre genre;
    private final String telephone;

    protected Personne(String id, String nom, String prenom, String pseudo,
                       Genre genre, String telephone) {
        this.id = Verifications.texte(id);
        this.nom = Verifications.texte(nom);
        this.prenom = Verifications.texte(prenom);
        this.pseudo = Verifications.texte(pseudo);
        if (genre == null) {
            throw new IllegalArgumentException("Le genre doit être renseigné");
        }
        this.genre = genre;
        this.telephone = Verifications.texte(telephone);
    }

    public String id() {
        return id;
    }

    public String nom() {
        return nom;
    }

    public String prenom() {
        return prenom;
    }

    public String pseudo() {
        return pseudo;
    }

    public Genre genre() {
        return genre;
    }

    public String telephone() {
        return telephone;
    }

    // Le sujet suppose qu'un identifiant désigne une seule personne.
    @Override
    public final boolean equals(Object objet) {
        if (!(objet instanceof Personne)) {
            return false;
        }
        Personne autre = (Personne) objet;
        return id.equals(autre.id);
    }

    @Override
    public final int hashCode() {
        return id.hashCode();
    }
}
