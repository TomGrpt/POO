package fr.imt.arena.metier;

import fr.imt.arena.util.Textes;

import java.util.Objects;

// Identité commune
public abstract class Personne {
    private final int id;
    private final String nom;
    private final String prenom;
    private final String pseudo;
    private final Genre genre;
    private final String telephone;

    protected Personne(
            int id, String nom, String prenom, String pseudo, Genre genre, String telephone) {
        this.id = id;
        this.nom = Textes.verifier(nom);
        this.prenom = Textes.verifier(prenom);
        this.pseudo = Textes.verifier(pseudo);
        this.genre = Objects.requireNonNull(genre);
        this.telephone = Textes.verifier(telephone);
    }

    public int getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public String getPseudo() {
        return pseudo;
    }

    public Genre getGenre() {
        return genre;
    }

    public String getTelephone() {
        return telephone;
    }

    @Override
    public String toString() {
        return pseudo;
    }
}
