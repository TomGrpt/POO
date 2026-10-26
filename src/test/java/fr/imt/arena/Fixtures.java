package fr.imt.arena;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

final class Fixtures {
    static final Jeu SOLO = new JeuSolo("Duel", 3);
    static Joueur joueur(String id) { return new Joueur(id, "Nom", "Prenom", id, Genre.NON_PRECISE, "0100", StatutJoueur.AMATEUR); }
    static Coach coach(String id) { return new Coach(id, "Nom", "Prenom", id, Genre.NON_PRECISE, "0100"); }
    static Arbitre arbitre(Jeu jeu) { return new Arbitre("arbitre", "Nom", "Prenom", "Ref", Genre.NON_PRECISE, "0100", Set.of(jeu)); }
    static Equipe equipe(String nom, int joueurs) {
        Equipe e = new Equipe(nom); e.nommer(coach("coach-" + nom));
        for (int i = 0; i < joueurs; i++) e.ajouter(joueur(nom + i));
        return e;
    }
    static Tournoi tournoi(String nom, int nombre, int arenes) {
        Tournoi t = new Tournoi(nom, SOLO, LocalDateTime.of(2026, 11, 1, 12, 0), BigDecimal.TEN, arenes);
        for (int i = 0; i < nombre; i++) t.inscrire(equipe(nom + i, 1));
        return t;
    }
    static ParametresSimulation rapide(double incidents) { return new ParametresSimulation(Duration.ZERO, incidents, Strategies.equitable()); }
    static final class Memoire implements DepotRapports {
        final List<Rapport> rapports = new CopyOnWriteArrayList<>();
        public void sauvegarder(Rapport r) { rapports.add(r); }
        public List<Rapport> tous() { return List.copyOf(rapports); }
        public List<Rapport> parJeu(String jeu) { return rapports.stream().filter(r -> r.jeu().equals(jeu)).toList(); }
    }
}
