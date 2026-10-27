package fr.imt.arena;

import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.TimeUnit;

final class Simulateur {
    private Simulateur() {
    }

    static void executer(Tournoi tournoi, ParametresSimulation parametres, Random random,
                         List<EcouteurMatch> observateurs, Queue<RuntimeException> erreurs)
            throws InterruptedException {
        List<Match> matchs = tournoi.matchs();
        long debut = System.nanoTime();
        long dureeTotale = parametres.dureeNanos(matchs.size(), tournoi.arenes());

        for (int premier = 0; premier < matchs.size(); premier = premier + tournoi.arenes()) {
            int fin = Math.min(premier + tournoi.arenes(), matchs.size());
            int numeroLot = premier / tournoi.arenes() + 1;
            long finDuLot = Math.multiplyExact(parametres.tempsParMatch().toNanos(), numeroLot);
            // Le dernier lot peut être plus court pour respecter la formule M * t / A.
            finDuLot = Math.min(finDuLot, dureeTotale);
            long tempsEcoule = System.nanoTime() - debut;
            long tempsRestant = finDuLot - tempsEcoule;
            if (tempsRestant > 0) {
                TimeUnit.NANOSECONDS.sleep(tempsRestant);
            }

            for (int i = premier; i < fin; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Simulation interrompue");
                }
                Match match = matchs.get(i);
                boolean incident = random.nextDouble() < parametres.probabiliteIncident();
                tournoi.terminerMatch(match, incident, parametres.strategie(), random);
                notifier(match, observateurs, erreurs);
            }
        }
    }

    private static void notifier(Match match, List<EcouteurMatch> observateurs,
                                 Queue<RuntimeException> erreurs) {
        for (EcouteurMatch observateur : observateurs) {
            try {
                observateur.matchTermine(match);
            } catch (RuntimeException erreur) {
                // Un affichage défaillant ne doit pas empêcher les autres notifications.
                erreurs.add(erreur);
            }
        }
    }
}
