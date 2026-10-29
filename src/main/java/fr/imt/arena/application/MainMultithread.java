package fr.imt.arena.application;

import fr.imt.arena.jeux.Jeu;
import fr.imt.arena.jeux.JeuEquipe;
import fr.imt.arena.metier.*;
import fr.imt.arena.persistance.DepotRapports;
import fr.imt.arena.strategies.TirageEquitable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainMultithread {
    public static void main(String[] args) throws Exception {
        try (DepotRapports depot = new DepotRapports();
             GestionnaireTournois gestion = new GestionnaireTournois(depot)) {
            List<Tournoi> tournois = preparer(gestion);
            long debut = System.nanoTime();

            // Un thread par tournoi
            for (int i = 0; i < 3; i++) {
                gestion.lancerSuivant(42 + i, new TirageEquitable(), ModeExecution.THREADS);
            }

            // Attente après les lancements
            for (Tournoi tournoi : tournois) {
                System.out.println(tournoi.attendreRapport(10, TimeUnit.SECONDS));
            }

            long duree = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - debut);
            System.out.println("Durée : " + duree + " ms");
        }
    }

    // Participants propres aux tournois
    private static List<Tournoi> preparer(GestionnaireTournois gestion) {
        Jeu jeu = new JeuEquipe("Rocket League", 3, 3);
        gestion.ajouterJeu(jeu);
        List<Tournoi> tournois = new ArrayList<>();
        int id = 1;

        for (int numero = 1; numero <= 3; numero++) {
            Tournoi tournoi = new Tournoi("Coupe " + numero, jeu,
                    LocalDate.now().plusDays(numero - 1), 500, 1);

            for (String nom : List.of("Alpha", "Bravo", "Charlie", "Delta")) {
                String nomEquipe = "T" + numero + "-" + nom;
                Equipe equipe = new Equipe(nomEquipe);
                Coach coach = new Coach(id++, "Coach", nom, "Coach-" + nomEquipe,
                        Genre.NON_PRECISE, "0600000000");
                gestion.ajouterPersonne(coach);
                equipe.definirCoach(coach);

                for (int j = 1; j <= 3; j++) {
                    Joueur joueur = new Joueur(id++, "Joueur", nom, nomEquipe + j,
                            Genre.NON_PRECISE, "0600000000");
                    gestion.ajouterPersonne(joueur);
                    equipe.ajouterJoueur(joueur);
                }
                gestion.ajouterEquipe(equipe);
                tournoi.inscrire(equipe);
            }
            Arbitre arbitre = new Arbitre(id++, "Arbitre", "Alex", "Arbitre" + numero,
                    Genre.NON_PRECISE, "0600000000", jeu);
            gestion.ajouterPersonne(arbitre);
            gestion.planifier(tournoi);
            tournois.add(tournoi);
        }
        return tournois;
    }
}
