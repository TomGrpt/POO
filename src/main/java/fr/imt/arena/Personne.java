package fr.imt.arena;

import java.util.Objects;

public abstract class Personne {
    private final String id, nom, prenom, pseudo, telephone;
    private final Genre genre;
    protected Personne(String id, String nom, String prenom, String pseudo, Genre genre, String telephone) {
        this.id = Verifications.texte(id); this.nom = Verifications.texte(nom);
        this.prenom = Verifications.texte(prenom); this.pseudo = Verifications.texte(pseudo);
        this.genre = Objects.requireNonNull(genre); this.telephone = Verifications.texte(telephone);
    }
    public final String id() { return id; }
    public final String nom() { return nom; }
    public final String prenom() { return prenom; }
    public final String pseudo() { return pseudo; }
    public final Genre genre() { return genre; }
    public final String telephone() { return telephone; }
    @Override public final boolean equals(Object o) { return o instanceof Personne p && id.equals(p.id); }
    @Override public final int hashCode() { return id.hashCode(); }
}
