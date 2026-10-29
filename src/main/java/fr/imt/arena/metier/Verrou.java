package fr.imt.arena.metier;

// Synchronisation commune
final class Verrou {
    static final Object OBJET = new Object();

    private Verrou() {}
}
