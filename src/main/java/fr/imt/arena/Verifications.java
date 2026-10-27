package fr.imt.arena;

final class Verifications {
    private Verifications() {
    }

    static String texte(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException("Le texte ne doit pas être vide");
        }
        return valeur.strip();
    }
}
