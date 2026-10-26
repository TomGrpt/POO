package fr.imt.arena;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

final class Simulateur {
    private Simulateur() { }
    static void executer(Tournoi tournoi, ParametresSimulation parametres, Random random,
                         List<EcouteurMatch> observateurs, Consumer<RuntimeException> erreursObservateurs) throws InterruptedException {
        List<Match> matchs = tournoi.matchs();
        long debut = System.nanoTime();
        long duree = parametres.dureeNanos(matchs.size(), tournoi.arenes());
        for (int premier = 0; premier < matchs.size(); premier += tournoi.arenes()) {
            int fin = Math.min(premier + tournoi.arenes(), matchs.size());
            // Dernier lot proratisé pour respecter exactement M*t/A (hors surcoûts CPU).
            long cible = Math.min(Math.multiplyExact(parametres.tempsParMatch().toNanos(), premier / tournoi.arenes() + 1L), duree);
            long restant = cible - (System.nanoTime() - debut);
            if (restant > 0) TimeUnit.NANOSECONDS.sleep(restant);
            for (int i = premier; i < fin; i++) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                Match match = matchs.get(i);
                tournoi.terminerMatch(match, random.nextDouble() < parametres.probabiliteIncident(), parametres.strategie(), random);
                for (EcouteurMatch observateur : observateurs) {
                    try { observateur.matchTermine(match); }
                    catch (RuntimeException e) { erreursObservateurs.accept(e); }
                }
            }
        }
    }
}
