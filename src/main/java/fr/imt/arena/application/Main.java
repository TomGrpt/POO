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
import fr.imt.arena.metier.Tournoi;
import fr.imt.arena.persistance.DepotRapports;
import fr.imt.arena.strategies.MeilleureEquipe;
import fr.imt.arena.strategies.TirageEquitable;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

// Programme de démonstration
public final class Main {
    public static void main(String[] args) throws Exception {

        // Ressources fermées automatiquement
        try (DepotRapports depot = new DepotRapports();
                GestionnaireTournois gestion = new GestionnaireTournois(depot)) {
            Jeu jeu = new JeuEquipe("Rocket League", 3, 3);
            gestion.ajouterJeu(jeu);
            gestion.ajouterJeu(new JeuSolo("Combat", 3));
            Tournoi tournoi = new Tournoi("Coupe IMT", jeu, LocalDate.now(), 500, 3);
            int id = 1;
            for (String nom : List.of("Alpha", "Bravo", "Charlie", "Delta")) {
                Equipe equipe = new Equipe(nom);
                Coach coach =
                        new Coach(
                                id++, "Coach", nom, "Coach" + nom, Genre.NON_PRECISE, "0600000000");
                gestion.ajouterPersonne(coach);
                equipe.definirCoach(coach);
                for (int j = 1; j <= 3; j++) {
                    Joueur joueur =
                            new Joueur(
                                    id++, "Joueur", nom, nom + j, Genre.NON_PRECISE, "0600000000");
                    gestion.ajouterPersonne(joueur);
                    equipe.ajouterJoueur(joueur);
                }
                gestion.ajouterEquipe(equipe);
                tournoi.inscrire(equipe);
            }
            Arbitre arbitre =
                    new Arbitre(
                            id, "Arbitre", "Alex", "Sifflet", Genre.NON_PRECISE, "0600000000", jeu);
            gestion.ajouterPersonne(arbitre);
            gestion.planifier(tournoi);
            Tournoi suivant = new Tournoi("Revanche", jeu, LocalDate.now().plusDays(1), 800, 2);
            for (Equipe equipe : tournoi.getEquipes()) {
                suivant.inscrire(equipe);
            }
            gestion.planifier(suivant);
            System.out.println("Planifiés : " + gestion.tournoisPlanifies());
            System.out.println("Arbitres libres : " + gestion.arbitres(false));
            // Premier tournoi
            gestion.lancerSuivant(42, new TirageEquitable());
            System.out.println("Le main continue. Etat : " + tournoi.getEtat());
            System.out.println("Arbitres occupés : " + gestion.arbitres(true));
            System.out.println(tournoi.attendreRapport(10, TimeUnit.SECONDS));
            // Second tournoi
            gestion.lancerSuivant(43, new MeilleureEquipe());
            System.out.println(suivant.attendreRapport(10, TimeUnit.SECONDS));
            for (Equipe equipe : gestion.classement()) {
                System.out.println(
                        equipe
                                + " : "
                                + equipe.getPoints()
                                + " points, coach : "
                                + equipe.getCoach().getVictoires()
                                + " victoires");
            }
            System.out.println("Terminés (budget décroissant) : " + gestion.tournoisTermines());
            System.out.println(
                    "Matchs Alpha : " + gestion.matchsEquipe(gestion.equipeParNom("Alpha")));
            System.out.println("Par vainqueur : " + tournoi.matchsParVainqueur());
            System.out.println("Matchs arbitrés : " + arbitre.getMatchsArbitres());
            for (Joueur joueur : gestion.equipeParNom("Alpha").getJoueurs()) {
                System.out.println(joueur + " : " + joueur.getStatut());
            }
            System.out.println("Tous les rapports persistés : " + depot.tous());
            System.out.println("Rapports Rocket League : " + depot.parJeu(jeu.getNom()));
        }
    }
}
