package fr.imt.arena;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static fr.imt.arena.Fixtures.*;

class TournamentTest {
    Arena arena;
    @BeforeEach void ouvrir() { arena = new Arena(new Memoire()); arena.ajouter(arbitre(SOLO)); }
    @AfterEach void fermer() { arena.close(); }
    @ParameterizedTest @ValueSource(ints = {4, 5, 6}) void roundRobin(int n) throws Exception {
        Tournoi t = tournoi("T", n, 3); arena.planifier(t); arena.lancerSuivant(rapide(0), 42).attendreRapport(Duration.ofSeconds(3));
        assertEquals(n * (n - 1) / 2, t.matchs().size());
        assertEquals(n - 1, t.matchs().stream().filter(m -> m.concerne(t.equipes().getFirst())).count());
        assertEquals(t.matchs().size(), t.matchs().stream().map(m -> m.premiere().nom() + "/" + m.seconde().nom()).distinct().count());
    }
    @Test void minimumEquipes() {
        arena.planifier(tournoi("Petit", 3, 1)); assertThrows(IllegalStateException.class, () -> arena.lancerSuivant(rapide(0), 1));
        assertEquals(1, arena.planifies().size()); assertEquals(0, arena.nombreEnCours());
    }
    @Test void coachEtEffectifObligatoiresRollback() {
        Tournoi t = tournoi("T", 4, 1); t.equipes().getLast().nommer(null); arena.planifier(t);
        assertThrows(IllegalStateException.class, () -> arena.lancerSuivant(rapide(0), 1));
        assertTrue(t.equipes().stream().noneMatch(Equipe::engagee));
        t.equipes().getLast().nommer(coach("c"));
        t.equipes().getLast().retirer(t.equipes().getLast().joueurs().getFirst());
        assertThrows(IllegalStateException.class, () -> arena.lancerSuivant(rapide(0), 1));
    }
    @Test void arbitreObligatoire() {
        try (Arena sansArbitre = new Arena(new Memoire())) {
            sansArbitre.planifier(tournoi("T", 4, 1));
            assertThrows(IllegalStateException.class, () -> sansArbitre.lancerSuivant(rapide(0), 1));
        }
    }
    @Test void ordreChronologiqueBudgetEtRegistre() throws Exception {
        Tournoi tard = tournoi("Tard", 4, 1);
        Tournoi tot = new Tournoi("Tot", SOLO, tard.date().minusDays(1), new BigDecimal("200"), 1);
        for (int i = 0; i < 4; i++) tot.inscrire(equipe("Tot" + i, 1));
        arena.planifier(tard); arena.planifier(tot);
        assertEquals(java.util.List.of(tot, tard), arena.planifies());
        assertSame(tot, arena.lancerSuivant(rapide(0), 1)); tot.attendreRapport(Duration.ofSeconds(3));
        arena.lancerSuivant(rapide(0), 1).attendreRapport(Duration.ofSeconds(3));
        assertEquals(java.util.List.of(tot, tard), arena.termines());
        Equipe e = equipe("Z", 1); arena.ajouter(e); assertSame(e, arena.equipe("Z").orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> arena.ajouter(new Equipe("Z")));
        assertThrows(UnsupportedOperationException.class, () -> arena.classement().clear());
    }
    @Test void parametresInvalidesEtDoublons() {
        assertThrows(IllegalArgumentException.class, () -> new Tournoi("T", SOLO, LocalDateTime.now(), BigDecimal.ONE, 5));
        assertThrows(IllegalArgumentException.class, () -> new Tournoi("T", SOLO, LocalDateTime.now(), BigDecimal.valueOf(-1), 1));
        Tournoi t = tournoi("T", 4, 1);
        assertThrows(IllegalArgumentException.class, () -> t.inscrire(t.equipes().getFirst()));
        arena.planifier(t); assertThrows(IllegalArgumentException.class, () -> arena.planifier(t));
    }
}
