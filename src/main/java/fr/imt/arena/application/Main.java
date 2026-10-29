package fr.imt.arena.application;

import fr.imt.arena.jeux.Jeu;
import fr.imt.arena.jeux.JeuEquipe;
import fr.imt.arena.jeux.JeuSolo;
import fr.imt.arena.metier.Arbitre;
import fr.imt.arena.metier.Coach;
import fr.imt.arena.metier.Equipe;
import fr.imt.arena.metier.Genre;
import fr.imt.arena.metier.GestionnaireTournois;
import fr.imt.arena.metier.Joueur;
import fr.imt.arena.metier.ModeExecution;
import fr.imt.arena.metier.Tournoi;
import fr.imt.arena.persistance.DepotRapports;
import fr.imt.arena.strategies.TirageEquitable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

// Comparaison des exécutions
public final class Main {
    private static final int NOMBRE_TOURNOIS = 3;

    public static void main(String[] args) throws Exception {
        String choix = "tous";
        if (args.length > 0) {
            choix = args[0].toLowerCase(Locale.ROOT);
        }
        if (!List.of("tous", "sequentiel", "threads", "pool").contains(choix)) {
            throw new IllegalArgumentException("Choisir : tous, sequentiel, threads ou pool");
        }

        // Fermeture automatique du dépôt
        try (DepotRapports depot = new DepotRapports()) {
            if (choix.equals("tous") || choix.equals("sequentiel")) {
                executerMode(depot, ModeExecution.SEQUENTIEL);
            }
            if (choix.equals("tous") || choix.equals("threads")) {
                executerMode(depot, ModeExecution.THREADS);
            }
            if (choix.equals("tous") || choix.equals("pool")) {
                executerMode(depot, ModeExecution.POOL);
            }
            System.out.println("Rapports enregistrés : " + depot.tous().size());
            System.out.println("Rapports Rocket League : " + depot.parJeu("Rocket League"));
        }
    }

    // Données neuves par mode
    private static void executerMode(DepotRapports depot, ModeExecution mode) throws Exception {
        System.out.println("\n=== " + mode + " ===");
        try (GestionnaireTournois gestion = new GestionnaireTournois(depot)) {
            List<Tournoi> tournois = creerTournois(gestion, mode);
            System.out.println("Planifiés : " + gestion.tournoisPlanifies());
            System.out.println("Arbitres libres : " + gestion.arbitres(false).size());
            long debut = System.nanoTime();

            if (mode == ModeExecution.SEQUENTIEL) {
                lancerSequentiel(gestion);
            } else if (mode == ModeExecution.THREADS) {
                lancerAvecThreads(gestion, tournois);
            } else {
                lancerAvecPool(gestion, tournois);
            }

            long dureeMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - debut);
            System.out.println("Durée observée : " + dureeMs + " ms (sauvegarde comprise)");
            afficherBilan(gestion, tournois.get(0));
        }
    }

    // Un tournoi après l’autre
    private static void lancerSequentiel(GestionnaireTournois gestion) throws Exception {
        System.out.println("Exécution dans : " + Thread.currentThread().getName());
        for (int i = 0; i < NOMBRE_TOURNOIS; i++) {
            Tournoi tournoi =
                    gestion.lancerSuivant(42 + i, new TirageEquitable(), ModeExecution.SEQUENTIEL);
            System.out.println(tournoi.attendreRapport(10, TimeUnit.SECONDS));
        }
    }

    // Un thread par tournoi
    private static void lancerAvecThreads(GestionnaireTournois gestion, List<Tournoi> tournois)
            throws Exception {
        for (int i = 0; i < NOMBRE_TOURNOIS; i++) {
            gestion.lancerSuivant(42 + i, new TirageEquitable(), ModeExecution.THREADS);
        }
        System.out.println("Tournois actifs après lancement : " + gestion.getNombreEnCours());
        System.out.println("Arbitres occupés : " + gestion.arbitres(true).size());
        attendreRapports(tournois);
    }

    // Trois workers réutilisables
    private static void lancerAvecPool(GestionnaireTournois gestion, List<Tournoi> tournois)
            throws Exception {
        for (int i = 0; i < NOMBRE_TOURNOIS; i++) {
            gestion.lancerSuivant(42 + i, new TirageEquitable(), ModeExecution.POOL);
        }
        System.out.println("Tournois actifs après lancement : " + gestion.getNombreEnCours());
        System.out.println("Arbitres occupés : " + gestion.arbitres(true).size());
        attendreRapports(tournois);
    }

    // Attente après les lancements
    private static void attendreRapports(List<Tournoi> tournois) throws Exception {
        for (Tournoi tournoi : tournois) {
            System.out.println(tournoi.attendreRapport(10, TimeUnit.SECONDS));
        }
    }

    // Participants indépendants par tournoi
    private static List<Tournoi> creerTournois(GestionnaireTournois gestion, ModeExecution mode) {
        Jeu jeu = new JeuEquipe("Rocket League", 3, 3);
        gestion.ajouterJeu(jeu);
        gestion.ajouterJeu(new JeuSolo("Combat", 3));
        List<Tournoi> tournois = new ArrayList<>();
        int id = 1;
        for (int numero = 1; numero <= NOMBRE_TOURNOIS; numero++) {
            Tournoi tournoi =
                    new Tournoi(
                            mode + " - Coupe " + numero,
                            jeu,
                            LocalDate.now().plusDays(numero - 1),
                            500 + numero * 100,
                            1);
            for (String nom : List.of("Alpha", "Bravo", "Charlie", "Delta")) {
                String nomEquipe = "T" + numero + "-" + nom;
                Equipe equipe = new Equipe(nomEquipe);
                Coach coach =
                        new Coach(
                                id++,
                                "Coach",
                                nom,
                                "Coach-" + nomEquipe,
                                Genre.NON_PRECISE,
                                "0600000000");
                gestion.ajouterPersonne(coach);
                equipe.definirCoach(coach);
                for (int joueurNumero = 1; joueurNumero <= 3; joueurNumero++) {
                    Joueur joueur =
                            new Joueur(
                                    id++,
                                    "Joueur",
                                    nom,
                                    nomEquipe + joueurNumero,
                                    Genre.NON_PRECISE,
                                    "0600000000");
                    gestion.ajouterPersonne(joueur);
                    equipe.ajouterJoueur(joueur);
                }
                gestion.ajouterEquipe(equipe);
                tournoi.inscrire(equipe);
            }
            Arbitre arbitre =
                    new Arbitre(
                            id++,
                            "Arbitre",
                            "Alex",
                            "Sifflet" + numero,
                            Genre.NON_PRECISE,
                            "0600000000",
                            jeu);
            gestion.ajouterPersonne(arbitre);
            gestion.planifier(tournoi);
            tournois.add(tournoi);
        }
        return tournois;
    }

    // Consultation des résultats
    private static void afficherBilan(GestionnaireTournois gestion, Tournoi premier) {
        System.out.println("Classement général :");
        for (Equipe equipe : gestion.classement()) {
            System.out.println(
                    equipe
                            + " : "
                            + equipe.getPoints()
                            + " points, coach : "
                            + equipe.getCoach().getVictoires()
                            + " victoires");
        }
        Equipe exemple = premier.getEquipes().get(0);
        System.out.println("Arbitres libres : " + gestion.arbitres(false).size());
        System.out.println("Terminés par budget : " + gestion.tournoisTermines());
        System.out.println("Matchs de " + exemple + " : " + gestion.matchsEquipe(exemple));
        System.out.println("Par vainqueur : " + premier.matchsParVainqueur());
        for (Joueur joueur : exemple.getJoueurs()) {
            System.out.println(joueur + " : " + joueur.getStatut());
        }
    }
}
