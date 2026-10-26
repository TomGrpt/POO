package fr.imt.arena;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static fr.imt.arena.Fixtures.*;

class PersistenceTest {
    @TempDir Path dossier;
    @Test void sauvegardeAutomatiqueEtReouvertureFichier() throws Exception {
        String url = "jdbc:h2:file:" + dossier.resolve("arena");
        Long id;
        try (DepotJpa depot = new DepotJpa(url); Arena arena = new Arena(depot)) {
            arena.ajouter(arbitre(SOLO)); Tournoi t = tournoi("Persistant", 4, 2); arena.planifier(t);
            Rapport r = arena.lancerSuivant(rapide(0), 42).attendreRapport(Duration.ofSeconds(5));
            assertNotNull(r.id()); id = r.id(); assertEquals(6, r.matchsJoues()); assertTrue(r.reussite());
        }
        assertTrue(Files.exists(dossier.resolve("arena.mv.db")));
        try (DepotJpa depot = new DepotJpa(url)) {
            assertEquals(1, depot.tous().size()); Rapport r = depot.parJeu("Duel").getFirst();
            assertEquals(id, r.id()); assertEquals("Persistant", r.nomTournoi());
            assertEquals(6, r.matchsPrevus()); assertEquals(6, r.matchsJoues()); assertNotNull(r.equipeVainqueur());
            assertEquals(LocalDateTime.of(2026, 11, 1, 12, 0), r.date());
            assertTrue(depot.parJeu("inconnu").isEmpty());
        }
    }
    @Test void troisSauvegardesDepuisLesWorkers() throws Exception {
        String url = "jdbc:h2:file:" + dossier.resolve("concurrent");
        try (DepotJpa depot = new DepotJpa(url); Arena arena = new Arena(depot)) {
            arena.ajouter(arbitre(SOLO));
            java.util.List<Tournoi> tournois = new java.util.ArrayList<>();
            for (int i = 0; i < 3; i++) { Tournoi t = tournoi("T" + i, 4, 1); tournois.add(t); arena.planifier(t); }
            for (int i = 0; i < 3; i++) arena.lancerSuivant(rapide(0), i);
            for (Tournoi t : tournois) assertNotNull(t.attendreRapport(Duration.ofSeconds(5)).id());
            assertEquals(3, depot.tous().size());
        }
        try (DepotJpa depot = new DepotJpa(url)) { assertEquals(3, depot.parJeu("Duel").size()); }
    }
}
