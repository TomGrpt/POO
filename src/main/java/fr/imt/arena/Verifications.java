package fr.imt.arena;

final class Verifications {
    private Verifications() { }
    static String texte(String valeur) {
        if (valeur == null || valeur.isBlank()) throw new IllegalArgumentException("Texte vide");
        return valeur.strip();
    }
}
