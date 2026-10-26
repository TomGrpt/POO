package fr.imt.arena;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class Main {
    private Main() { }
    public static void main(String[] args) throws Exception {
        try (DepotJpa depot = new DepotJpa(); Arena arena = new Arena(depot)) {
            Jeu solo = new JeuSolo("Duel", 3), collectif = new JeuCollectif("Arena 3", 5, 3);
            arena.ajouter(solo); arena.ajouter(collectif);
            for (int i = 1; i <= 2; i++) arena.ajouter(new Arbitre("a" + i, "Arbitre", "Alex", "Ref" + i,
                Genre.NON_PRECISE, "0100000000", Set.of(solo, collectif)));
            List<Equipe> equipes = new ArrayList<>();
            for (int i = 1; i <= 6; i++) {
                Equipe e = new Equipe("Equipe " + i);
                Coach coach = new Coach("c" + i, "Coach", "Camille", "Coach" + i, Genre.NON_PRECISE, "0100000000");
                arena.ajouter(coach); e.nommer(coach);
                for (int j = 1; j <= 3; j++) {
                    Joueur p = new Joueur("j" + i + "-" + j, "Joueur", "Sam", "P" + i + j,
                        Genre.NON_PRECISE, "0100000000", StatutJoueur.AMATEUR);
                    arena.ajouter(p); e.ajouter(p);
                }
                arena.ajouter(e); equipes.add(e);
            }
            LocalDateTime date = LocalDateTime.of(2026, 11, 1, 14, 0);
            Tournoi premier = new Tournoi("Coupe Duel", solo, date, new BigDecimal("1000"), 3);
            Tournoi second = new Tournoi("Coupe Collective", collectif, date.plusDays(1), new BigDecimal("2000"), 2);
            equipes.forEach(e -> { premier.inscrire(e); second.inscrire(e); });
            arena.planifier(second); arena.planifier(premier);
            System.out.println("Planifiés : " + arena.planifies().stream().map(Tournoi::nom).toList());
            java.util.concurrent.atomic.AtomicInteger notifications = new java.util.concurrent.atomic.AtomicInteger();
            arena.observer(m -> notifications.incrementAndGet());
            ParametresSimulation parametres = new ParametresSimulation(Duration.ofMillis(100), 0.10, Strategies.equitable());
            Tournoi lance = arena.lancerSuivant(parametres, 42);
            System.out.println("Etat : " + lance.etat() + ", arbitres occupés : " + arena.arbitres(false).size());
            System.out.println("Rapport : " + lance.attendreRapport(Duration.ofSeconds(10)));
            System.out.println("Classement local : " + lance.classement() + ", points : " + lance.pointsTournoi());
            System.out.println("Matchs par gagnant : " + lance.matchsParVainqueur().entrySet().stream()
                .map(e -> e.getKey().nom() + "=" + e.getValue().size()).toList());
            Tournoi autre = arena.lancerSuivant(parametres, 84);
            System.out.println("Rapport : " + autre.attendreRapport(Duration.ofSeconds(10)));
            System.out.println("Classement général : " + arena.classement().stream().map(e -> e.nom() + ": " + e.points()).toList());
            System.out.println("Arbitres disponibles : " + arena.arbitres(true).size());
            System.out.println("Matchs de l'équipe 1 : " + arena.matchs(equipes.getFirst()).size());
            System.out.println("Terminés (budget décroissant) : " + arena.termines().stream().map(Tournoi::nom).toList());
            System.out.println("Rapports enregistrés : " + depot.tous());
            System.out.println("Rapports Duel : " + depot.parJeu("Duel"));
            System.out.println("Notifications Observer : " + notifications.get());
        }
    }
}
