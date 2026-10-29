package fr.imt.arena.util;

// Validation du texte
public final class Textes {
    private Textes() {}

    public static String verifier(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException("Texte vide");
        }
        return valeur.trim();
    }
}
