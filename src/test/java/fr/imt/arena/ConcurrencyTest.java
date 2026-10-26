package fr.imt.arena;

import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static fr.imt.arena.Fixtures.*;

class ConcurrencyTest {
    @Test void troisLancementsMaximumSousConcurrence() throws Exception {
        try (Arena arena = new Arena(new Memoire()); ExecutorService appels = Executors.newFixedThreadPool(8)) {
            arena.ajouter(arbitre(SOLO));
            for (int i = 0; i < 8; i++) arena.planifier(tournoi("T" + i, 4, 1));
            CountDownLatch depart = new CountDownLatch(1), libererWorkers = new CountDownLatch(1);
            arena.observer(m -> { try { libererWorkers.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } });
            List<Future<Boolean>> resultats = new ArrayList<>();
            try {
                for (int i = 0; i < 8; i++) resultats.add(appels.submit(() -> {
                    depart.await();
                    try { arena.lancerSuivant(rapide(0), 1); return true; }
                    catch (IllegalStateException e) { return false; }
                }));
                depart.countDown(); int acceptes = 0;
                for (Future<Boolean> resultat : resultats) if (resultat.get(3, TimeUnit.SECONDS)) acceptes++;
                assertEquals(3, acceptes); assertEquals(3, arena.nombreEnCours()); assertEquals(5, arena.planifies().size());
            } finally { libererWorkers.countDown(); }
        }
    }
    @Test void pasDeDoubleReservationEtPasDeMutation() throws Exception {
        try (Arena arena = new Arena(new Memoire())) {
            arena.ajouter(arbitre(SOLO)); Tournoi t1 = tournoi("A", 4, 1), t2 = tournoi("B", 0, 1);
            t1.equipes().forEach(t2::inscrire); arena.planifier(t1); arena.planifier(t2);
            CountDownLatch entre = new CountDownLatch(1), fin = new CountDownLatch(1);
            arena.observer(m -> { entre.countDown(); try { fin.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } });
            try {
                arena.lancerSuivant(rapide(0), 1); assertTrue(entre.await(3, TimeUnit.SECONDS));
                assertThrows(IllegalStateException.class, () -> arena.lancerSuivant(rapide(0), 2));
                Equipe e = t1.equipes().getFirst();
                assertThrows(IllegalStateException.class, () -> e.retirer(e.joueurs().getFirst()));
                assertThrows(IllegalStateException.class, () -> e.nommer(null));
                assertThrows(IllegalStateException.class, () -> e.ajouter(joueur("autre")));
                assertEquals(EtatTournoi.PLANIFIE, t2.etat());
            } finally { fin.countDown(); }
            t1.attendreRapport(Duration.ofSeconds(3));
            arena.lancerSuivant(rapide(0), 2).attendreRapport(Duration.ofSeconds(3));
        }
    }
    @Test void fermetureAttendLaFinEtInterditLesLancements() throws Exception {
        Memoire depot = new Memoire(); Arena arena = new Arena(depot); arena.ajouter(arbitre(SOLO));
        Tournoi t = tournoi("T", 4, 2); arena.planifier(t);
        arena.lancerSuivant(new ParametresSimulation(Duration.ofMillis(10), 0, Strategies.meilleure()), 1);
        arena.close(); assertEquals(EtatTournoi.TERMINE, t.etat()); assertEquals(1, depot.tous().size());
        assertEquals(0, arena.nombreEnCours()); assertNotNull(t.attendreRapport(Duration.ZERO));
        assertThrows(IllegalStateException.class, () -> arena.lancerSuivant(rapide(0), 1)); arena.close();
    }
    @Test void deuxEquipesConvoitentLeMemeJoueur() throws Exception {
        Equipe a = new Equipe("A"), b = new Equipe("B"); Joueur p = joueur("p");
        try (ExecutorService appels = Executors.newFixedThreadPool(2)) {
            CountDownLatch depart = new CountDownLatch(1);
            Callable<Boolean> premier = () -> { depart.await(); try { a.ajouter(p); return true; } catch (IllegalStateException e) { return false; } };
            Callable<Boolean> second = () -> { depart.await(); try { b.ajouter(p); return true; } catch (IllegalStateException e) { return false; } };
            Future<Boolean> x = appels.submit(premier), y = appels.submit(second); depart.countDown();
            assertNotEquals(x.get(3, TimeUnit.SECONDS), y.get(3, TimeUnit.SECONDS));
            assertEquals(1, a.joueurs().size() + b.joueurs().size());
        }
    }
    @Test void interruptionFermetureArreteLesWorkersEtRestaureLeFlag() throws Exception {
        Memoire depot = new Memoire(); Arena arena = new Arena(depot); arena.ajouter(arbitre(SOLO));
        Tournoi t = tournoi("Long", 4, 1); arena.planifier(t);
        arena.lancerSuivant(new ParametresSimulation(Duration.ofSeconds(60), 0, Strategies.meilleure()), 1);
        Thread.currentThread().interrupt();
        try { arena.close(); assertTrue(Thread.currentThread().isInterrupted()); }
        finally { Thread.interrupted(); }
        assertEquals(EtatTournoi.TERMINE, t.etat()); assertEquals(0, arena.nombreEnCours());
        assertTrue(t.equipes().stream().noneMatch(Equipe::engagee));
        assertThrows(ExecutionException.class, () -> t.attendreRapport(Duration.ofSeconds(1)));
        assertEquals(1, depot.tous().size());
    }
    @Test void photographieCoherentePendantLaSimulation() throws Exception {
        try (Arena arena = new Arena(new Memoire())) {
            arena.ajouter(arbitre(SOLO)); Tournoi t = tournoi("Vue", 6, 3); arena.planifier(t);
            VueTournoi debut = t.consulter();
            arena.lancerSuivant(new ParametresSimulation(Duration.ofMillis(10), 0.2, Strategies.equitable()), 42);
            int observations = 0;
            do {
                VueTournoi vue = t.consulter();
                long joues = vue.matchs().stream().filter(m -> m.etat() == EtatMatch.JOUE).count();
                assertEquals(joues * 3, vue.points().values().stream().mapToInt(Integer::intValue).sum());
                assertTrue(vue.matchs().stream().allMatch(m -> (m.etat() == EtatMatch.JOUE) == (m.vainqueur() != null)));
                observations++;
                Thread.yield();
            } while (t.etat() != EtatTournoi.TERMINE && observations < 100000);
            t.attendreRapport(Duration.ofSeconds(3)); assertTrue(observations > 0);
            assertEquals(0, debut.points().values().stream().mapToInt(Integer::intValue).sum());
            assertThrows(UnsupportedOperationException.class, () -> debut.points().clear());
        }
    }
}
