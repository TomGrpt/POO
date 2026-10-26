package fr.imt.arena;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static fr.imt.arena.Fixtures.*;

class DomainTest {
    Equipe a, b;
    @BeforeEach void preparer() { a = equipe("A", 3); b = equipe("B", 3); }
    @Test void appartenanceExclusiveEtTransfert() {
        Joueur p = a.joueurs().getFirst();
        assertThrows(IllegalStateException.class, () -> b.ajouter(p));
        a.retirer(p); b.ajouter(p); assertSame(b, p.equipe().orElseThrow());
        assertThrows(UnsupportedOperationException.class, () -> b.joueurs().clear());
    }
    @Test void coachExclusifEtCarriere() {
        Coach coach = a.coach().orElseThrow();
        assertThrows(IllegalStateException.class, () -> b.nommer(coach));
        a.victoire(); a.nommer(null); b.nommer(coach); b.victoire(); assertEquals(2, coach.victoires());
    }
    @Test void promotionApresTroisVictoiresEtResetArrivee() {
        Joueur p = a.joueurs().getFirst(); a.victoire(); a.victoire();
        assertEquals(StatutJoueur.AMATEUR, p.statut()); a.retirer(p); b.ajouter(p);
        b.victoire(); b.victoire(); assertEquals(StatutJoueur.AMATEUR, p.statut());
        b.victoire(); assertEquals(StatutJoueur.PROFESSIONNEL, p.statut());
        assertEquals(9, b.points()); assertEquals(3, b.coach().orElseThrow().victoires());
        b.retirer(p); a.ajouter(p); assertEquals(StatutJoueur.PROFESSIONNEL, p.statut());
    }
    @Test void qualificationEtMatchContreSoi() {
        assertThrows(IllegalArgumentException.class, () -> new Match(a, a, SOLO, arbitre(SOLO)));
        assertThrows(IllegalArgumentException.class, () -> new Match(a, b, SOLO, arbitre(new JeuSolo("Autre", 1))));
        assertThrows(IllegalArgumentException.class, () -> new Arbitre("a", "n", "p", "s", Genre.HOMME, "0", Set.of()));
    }
    @Test void victoireEtAnnulation() {
        Arbitre ref = arbitre(SOLO); Match match = new Match(a, b, SOLO, ref);
        match.terminer(false, Strategies.meilleure(), new Random(1));
        assertSame(a, match.vainqueur().orElseThrow()); assertEquals(3, a.points()); assertEquals(1, ref.matchsArbitres());
        assertThrows(IllegalStateException.class, () -> match.terminer(false, Strategies.meilleure(), new Random()));
        Match incident = new Match(a, b, SOLO, ref); incident.terminer(true, Strategies.meilleure(), new Random());
        assertEquals(EtatMatch.ANNULE, incident.etat()); assertTrue(incident.vainqueur().isEmpty());
        assertEquals(3, a.points()); assertEquals(1, ref.matchsArbitres());
    }
    @ParameterizedTest @ValueSource(ints = {3, 4, 5}) void compositionsCollectives(int taille) {
        Jeu jeu = new JeuCollectif("Collectif", 2, taille);
        assertEquals(taille, jeu.selectionner(equipe("C", 5).joueurs()).size());
        assertThrows(IllegalStateException.class, () -> jeu.selectionner(equipe("D", taille - 1).joueurs()));
    }
    @ParameterizedTest @ValueSource(ints = {-1, 0, 1, 2, 6}) void taillesInvalides(int taille) {
        assertThrows(IllegalArgumentException.class, () -> new JeuCollectif("C", 2, taille));
    }
    @Test void selectionSoloEtExtensionPolymorphe() {
        assertEquals(1, new Match(a, b, SOLO, arbitre(SOLO)).compositionPremiere().size());
        Jeu duo = new Jeu("Extension Duo", 1) { public int joueursParEquipe() { return 2; } };
        assertEquals(2, new Match(a, b, duo, arbitre(duo)).compositionSeconde().size());
    }
    @Test void strategiesEtVainqueurInvalide() {
        a.victoire();
        assertSame(a, Strategies.meilleure().choisir(a, b, new Random(3)));
        Random random = new Random(42); int victoires = 0;
        for (int i = 0; i < 10000; i++) if (Strategies.ponderee().choisir(a, b, random) == a) victoires++;
        assertTrue(victoires > 7500 && victoires < 8500);
        Match m = new Match(a, b, SOLO, arbitre(SOLO));
        assertThrows(IllegalStateException.class, () -> m.terminer(false, (x,y,r) -> equipe("X", 1), random));
        assertEquals(EtatMatch.PREVU, m.etat());
    }
}
