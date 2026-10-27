package fr.imt.arena;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        // Fermeture dans l'ordre inverse : attendre les tournois avant de fermer H2.
        try (DepotJpa depot = new DepotJpa(); Arena arena = new Arena(depot)) {
            Jeu solo = new JeuSolo("Duel", 3);
            Jeu collectif = new JeuCollectif("Arena 3", 5, 3);
            arena.ajouter(solo);
            arena.ajouter(collectif);
            creerArbitres(arena, solo, collectif);
            List<Equipe> equipes = creerEquipes(arena);

            LocalDateTime date = LocalDateTime.of(2026, 11, 1, 14, 0);
            Tournoi premier = new Tournoi("Coupe Duel", solo, date, new BigDecimal("1000"), 3);
            Tournoi second = new Tournoi("Coupe Collective", collectif,
                date.plusDays(1), new BigDecimal("2000"), 2);
            for (Equipe equipe : equipes) {
                premier.inscrire(equipe);
                second.inscrire(equipe);
            }
            // Le plus proche doit sortir en premier, même ajouté en dernier.
            arena.planifier(second);
            arena.planifier(premier);
            afficherTournois("Tournois planifiés", arena.planifies());

            CompteurMatchs compteur = new CompteurMatchs();
            arena.observer(compteur);
            ParametresSimulation parametres = new ParametresSimulation(
                Duration.ofMillis(100), 0.10, new StrategieEquitable());

            Tournoi lance = arena.lancerSuivant(parametres, 42);
            System.out.println("Etat : " + lance.etat());
            System.out.println("Arbitres occupés : " + arena.arbitres(false).size());
            Rapport rapport = lance.attendreRapport(Duration.ofSeconds(10));
            System.out.println("Rapport : " + rapport);
            afficherClassementLocal(lance);
            afficherMatchsGagnes(lance);

            // Les équipes étant communes, attendre la fin du premier avant le second.
            Tournoi autre = arena.lancerSuivant(parametres, 84);
            System.out.println("Rapport : " + autre.attendreRapport(Duration.ofSeconds(10)));
            System.out.println("Classement général :");
            for (Equipe equipe : arena.classement()) {
                System.out.println("  " + equipe.nom() + " : " + equipe.points() + " points");
            }
            System.out.println("Arbitres disponibles : " + arena.arbitres(true).size());
            System.out.println("Matchs de l'équipe 1 : " + arena.matchs(equipes.get(0)).size());
            afficherTournois("Tournois terminés par budget décroissant", arena.termines());
            System.out.println("Rapports enregistrés : " + depot.tous());
            System.out.println("Rapports Duel : " + depot.parJeu("Duel"));
            System.out.println("Notifications Observer : " + compteur.nombre());
        }
    }

    private static void creerArbitres(Arena arena, Jeu solo, Jeu collectif) {
        for (int i = 1; i <= 2; i++) {
            Arbitre arbitre = new Arbitre("a" + i, "Arbitre", "Alex", "Ref" + i,
                Genre.NON_PRECISE, "0100000000", Set.of(solo, collectif));
            arena.ajouter(arbitre);
        }
    }

    private static List<Equipe> creerEquipes(Arena arena) {
        List<Equipe> equipes = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            Equipe equipe = new Equipe("Equipe " + i);
            Coach coach = new Coach("c" + i, "Coach", "Camille", "Coach" + i,
                Genre.NON_PRECISE, "0100000000");
            arena.ajouter(coach);
            equipe.nommer(coach);
            for (int j = 1; j <= 3; j++) {
                Joueur joueur = new Joueur("j" + i + "-" + j, "Joueur", "Sam", "P" + i + j,
                    Genre.NON_PRECISE, "0100000000", StatutJoueur.AMATEUR);
                arena.ajouter(joueur);
                equipe.ajouter(joueur);
            }
            arena.ajouter(equipe);
            equipes.add(equipe);
        }
        return equipes;
    }

    private static void afficherTournois(String titre, List<Tournoi> tournois) {
        System.out.println(titre + " :");
        for (Tournoi tournoi : tournois) {
            System.out.println("  " + tournoi.nom());
        }
    }

    private static void afficherClassementLocal(Tournoi tournoi) {
        System.out.println("Classement de " + tournoi.nom() + " :");
        Map<Equipe, Integer> points = tournoi.pointsTournoi();
        for (Equipe equipe : tournoi.classement()) {
            System.out.println("  " + equipe.nom() + " : " + points.get(equipe) + " points");
        }
    }

    private static void afficherMatchsGagnes(Tournoi tournoi) {
        System.out.println("Matchs regroupés par vainqueur :");
        Map<Equipe, List<Match>> groupes = tournoi.matchsParVainqueur();
        for (Equipe equipe : groupes.keySet()) {
            int nombreVictoires = groupes.get(equipe).size();
            System.out.println("  " + equipe.nom() + " : " + nombreVictoires + " victoire(s)");
        }
    }
}
