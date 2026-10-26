package fr.imt.arena;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static fr.imt.arena.Fixtures.*;

class SimulationTest {
    @ParameterizedTest @CsvSource({"10,7,true", "10,6,false", "6,4,false", "6,5,true", "15,11,true", "15,10,false"})
    void seuilSansArrondi(int prevus, int joues, boolean reussi) { assertEquals(reussi, Rapport.estReussi(prevus, joues)); }
    @Test void incidentsEtObservateursIsoles() throws Exception {
        Memoire depot = new Memoire();
        try (Arena arena = new Arena(depot)) {
            Arbitre ref = arbitre(SOLO); arena.ajouter(ref); Tournoi t = tournoi("T", 4, 2); arena.planifier(t);
            AtomicInteger compte = new AtomicInteger(); arena.observer(m -> { throw new IllegalStateException("Observateur défaillant"); });
            arena.observer(m -> compte.incrementAndGet());
            Rapport rapport = arena.lancerSuivant(rapide(1), 42).attendreRapport(Duration.ofSeconds(3));
            assertEquals(6, compte.get()); assertEquals(6, arena.erreursObservateurs().size());
            assertEquals(0, rapport.matchsJoues()); assertNull(rapport.equipeVainqueur()); assertFalse(rapport.reussite());
            assertEquals(0, ref.matchsArbitres()); assertTrue(t.matchsParVainqueur().isEmpty());
            assertTrue(t.equipes().stream().allMatch(e -> e.points() == 0)); assertEquals(1, depot.tous().size());
        }
    }
    @Test void reproductibiliteEtGroupement() throws Exception {
        List<String> premier = simuler(42), second = simuler(42);
        assertEquals(premier, second); assertNotEquals(premier, simuler(84));
    }
    private List<String> simuler(long graine) throws Exception {
        try (Arena arena = new Arena(new Memoire())) {
            arena.ajouter(arbitre(SOLO)); Tournoi t = tournoi("T", 6, 3); arena.planifier(t);
            Rapport rapport = arena.lancerSuivant(rapide(0.2), graine).attendreRapport(Duration.ofSeconds(3));
            assertEquals(EtatTournoi.TERMINE, t.etat()); assertTrue(arena.arbitres(false).isEmpty());
            assertEquals(rapport.matchsJoues(), t.matchsParVainqueur().values().stream().mapToInt(List::size).sum());
            assertEquals(5, arena.matchs(t.equipes().getFirst()).size());
            assertEquals(rapport.matchsJoues() * 3, t.pointsTournoi().values().stream().mapToInt(Integer::intValue).sum());
            assertThrows(UnsupportedOperationException.class, () -> t.matchsParVainqueur().clear());
            assertThrows(IllegalStateException.class, () -> t.inscrire(equipe("New", 1)));
            return t.matchs().stream().map(m -> m.vainqueur().map(Equipe::nom).orElse("incident")).toList();
        }
    }
    @Test void formuleEtDureeReelle() throws Exception {
        ParametresSimulation p = new ParametresSimulation(Duration.ofMillis(100), 0, Strategies.meilleure());
        assertEquals(Duration.ofMillis(500).toNanos(), p.dureeNanos(15, 3));
        assertEquals(Duration.ofMillis(375).toNanos(), p.dureeNanos(15, 4));
        try (Arena arena = new Arena(new Memoire())) {
            arena.ajouter(arbitre(SOLO)); Tournoi t = tournoi("T", 6, 3); arena.planifier(t);
            long debut = System.nanoTime(); arena.lancerSuivant(p, 42);
            assertThrows(TimeoutException.class, () -> t.attendreRapport(Duration.ofMillis(1)));
            t.attendreRapport(Duration.ofSeconds(5));
            assertTrue(System.nanoTime() - debut >= Duration.ofMillis(490).toNanos());
        }
    }
    @Test void echecPersistanceObservableMaisReservationsLiberees() throws Exception {
        DepotRapports panne = new MemoirePanne();
        try (Arena arena = new Arena(panne)) {
            arena.ajouter(arbitre(SOLO)); Tournoi t = tournoi("T", 4, 1); arena.planifier(t); arena.lancerSuivant(rapide(0), 1);
            ExecutionException e = assertThrows(ExecutionException.class, () -> t.attendreRapport(Duration.ofSeconds(3)));
            assertEquals("Disque indisponible", e.getCause().getMessage());
            assertEquals(EtatTournoi.TERMINE, t.etat()); assertTrue(t.equipes().stream().noneMatch(Equipe::engagee));
            assertEquals(0, arena.nombreEnCours());
        }
    }
    @Test void strategieDefaillanteNeBloquePasCloture() throws Exception {
        Memoire depot = new Memoire();
        try (Arena arena = new Arena(depot)) {
            arena.ajouter(arbitre(SOLO)); Tournoi t = tournoi("T", 4, 1); arena.planifier(t);
            arena.lancerSuivant(new ParametresSimulation(Duration.ZERO, 0, (a,b,r) -> { throw new IllegalStateException("panne"); }), 1);
            assertThrows(ExecutionException.class, () -> t.attendreRapport(Duration.ofSeconds(3)));
            assertTrue(t.matchs().stream().allMatch(m -> m.etat() == EtatMatch.ANNULE));
            assertEquals(1, depot.tous().size()); assertTrue(t.equipes().stream().noneMatch(Equipe::engagee));
        }
    }
    static class MemoirePanne implements DepotRapports {
        public void sauvegarder(Rapport r) { throw new IllegalStateException("Disque indisponible"); }
        public List<Rapport> tous() { return List.of(); }
        public List<Rapport> parJeu(String jeu) { return List.of(); }
    }
}
