package fr.imt.arena.metier;

import static org.junit.jupiter.api.Assertions.*;

import fr.imt.arena.jeux.*;
import fr.imt.arena.persistance.*;
import fr.imt.arena.strategies.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

// Vérifications du projet
class ArenaTest {
    @TempDir Path dossier;
    DepotRapports depot;
    GestionnaireTournois gestion;
    Jeu jeu;
    int id;

    @BeforeEach
    // Préparation du test
    void preparer() {
        depot = new DepotRapports("jdbc:h2:" + dossier.resolve("arena"));
        gestion = new GestionnaireTournois(depot, 0, 0);
        jeu = new JeuEquipe("Rocket League", 3, 3);
        gestion.ajouterJeu(jeu);
        id = 1;
    }

    @AfterEach
    // Fermeture des ressources
    void fermer() {
        gestion.close();
        depot.close();
    }

    Equipe equipe(String nom) {
        Equipe e = new Equipe(nom);
        Coach c = new Coach(id++, "Nom", "Prénom", "Coach" + nom, Genre.NON_PRECISE, "06");
        gestion.ajouterPersonne(c);
        e.definirCoach(c);
        for (int n = 0; n < 3; n++) {
            Joueur j = new Joueur(id++, "Nom", "Prénom", nom + n, Genre.NON_PRECISE, "06");
            gestion.ajouterPersonne(j);
            e.ajouterJoueur(j);
        }
        gestion.ajouterEquipe(e);
        return e;
    }

    Arbitre arbitre() {
        Arbitre a = new Arbitre(id++, "Nom", "Prénom", "Arbitre", Genre.NON_PRECISE, "06", jeu);
        gestion.ajouterPersonne(a);
        return a;
    }

    Tournoi tournoi(String nom, int nombre, int jour) {
        Tournoi t = new Tournoi(nom, jeu, LocalDate.of(2026, 11, 1).plusDays(jour), 100 + jour, 3);
        for (int n = 0; n < nombre; n++) {
            t.inscrire(equipe(nom + n));
        }
        gestion.planifier(t);
        return t;
    }

    RapportTournoi attendre(Tournoi t) throws Exception {
        return t.attendreRapport(10, TimeUnit.SECONDS);
    }

    @Test
    void tournoiCompletPromotionEtPersistance() throws Exception {
        Tournoi t = tournoi("Coupe", 4, 0);
        Arbitre a = arbitre();
        gestion.lancerSuivant(42, new MeilleureEquipe());
        RapportTournoi r = attendre(t);
        assertEquals(6, r.getMatchsPrevus());
        assertEquals(6, r.getMatchsJoues());
        assertTrue(r.isReussite());
        assertEquals("Coupe0", r.getVainqueur());
        Equipe gagnante = gestion.equipeParNom("Coupe0");
        assertEquals(9, gagnante.getPoints());
        assertEquals(3, gagnante.getCoach().getVictoires());
        assertEquals(StatutJoueur.PRO, gagnante.getJoueurs().get(0).getStatut());
        assertEquals(6, a.getMatchsArbitres());
        assertFalse(a.estOccupe());
        assertEquals(EtatTournoi.TERMINE, t.getEtat());
        assertEquals(18, gestion.classement().stream().mapToInt(Equipe::getPoints).sum());
        assertEquals(3, gestion.matchsEquipe(gagnante).size());
        assertEquals(6, t.matchsParVainqueur().values().stream().mapToInt(List::size).sum());
        assertEquals(1, depot.parJeu(jeu.getNom()).size());
        assertTrue(depot.parJeu("Inconnu").isEmpty());
        depot.close();
        depot = new DepotRapports("jdbc:h2:" + dossier.resolve("arena"));
        assertEquals("Coupe0", depot.tous().get(0).getVainqueur());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 5, -1})
    void refuseArenesInvalides(int arenes) {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Tournoi("Invalide", jeu, LocalDate.now(), 100, arenes));
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 6})
    void refuseEffectifDeJeuInvalide(int nombre) {
        assertThrows(IllegalArgumentException.class, () -> new JeuEquipe("Jeu", 3, nombre));
    }

    @Test
    void refuseDemarrageSansArbitrePuisSansEffectif() {
        Tournoi t = tournoi("Coupe", 4, 0);
        assertThrows(
                IllegalStateException.class, () -> gestion.lancerSuivant(1, new TirageEquitable()));
        assertEquals(EtatTournoi.PLANIFIE, t.getEtat());
        assertEquals(0, gestion.getNombreEnCours());
        arbitre();
        Equipe e = t.getEquipes().get(0);
        e.retirerJoueur(e.getJoueurs().get(0));
        assertThrows(
                IllegalStateException.class, () -> gestion.lancerSuivant(1, new TirageEquitable()));
    }

    @Test
    void refuseMoinsDeQuatreEquipesEtCoachAbsent() {
        Tournoi t = tournoi("Coupe", 3, 0);
        arbitre();
        assertThrows(
                IllegalStateException.class, () -> gestion.lancerSuivant(1, new TirageEquitable()));
        Equipe sansCoach = new Equipe("Sans coach");
        gestion.ajouterEquipe(sansCoach);
        t.inscrire(sansCoach);
        assertThrows(
                IllegalStateException.class, () -> gestion.lancerSuivant(1, new TirageEquitable()));
    }

    @Test
    void transfertsEtListesProtegees() {
        Equipe a = equipe("A"), b = equipe("B");
        Joueur j = a.getJoueurs().get(0);
        b.ajouterJoueur(j);
        assertSame(b, j.getEquipe());
        assertFalse(a.getJoueurs().contains(j));
        Coach c = a.getCoach();
        b.definirCoach(c);
        assertNull(a.getCoach());
        assertSame(b, c.getEquipe());
        assertThrows(UnsupportedOperationException.class, () -> b.getJoueurs().clear());
        assertThrows(IllegalArgumentException.class, () -> gestion.ajouterEquipe(new Equipe("A")));
    }

    @Test
    void incidentsSansPoints() throws Exception {
        gestion.close();
        gestion = new GestionnaireTournois(depot, 0, 1);
        gestion.ajouterJeu(jeu);
        Tournoi t = tournoi("Panne", 4, 0);
        Arbitre a = arbitre();
        gestion.lancerSuivant(1, new TirageEquitable());
        RapportTournoi r = attendre(t);
        assertFalse(r.isReussite());
        assertEquals(0, r.getMatchsJoues());
        assertEquals("Aucun", r.getVainqueur());
        assertEquals(6, t.getMatchs().size());
        assertEquals(0, a.getMatchsArbitres());
        assertTrue(t.matchsParVainqueur().isEmpty());
        for (Equipe e : t.getEquipes()) {
            assertEquals(0, e.getPoints());
        }
    }

    @Test
    void seuilReussiteExact() {
        Tournoi t = tournoi("Seuil", 5, 0);
        assertFalse(new RapportTournoi(t, 6, "A").isReussite());
        assertTrue(new RapportTournoi(t, 7, "A").isReussite());
    }

    @Test
    void prioriteDatePuisTriBudget() throws Exception {
        Tournoi tard = tournoi("Tard", 4, 5);
        Tournoi tot = tournoi("Tot", 4, 0);
        arbitre();
        assertEquals(List.of(tot, tard), gestion.tournoisPlanifies());
        assertSame(tot, gestion.lancerSuivant(1, new TirageEquitable()));
        attendre(tot);
        assertSame(tard, gestion.lancerSuivant(1, new TirageEquitable()));
        attendre(tard);
        assertEquals(List.of(tard, tot), gestion.tournoisTermines());
    }

    @Test
    void memeGraineMemesResultats() throws Exception {
        Tournoi a = tournoi("A", 4, 0), b = tournoi("B", 4, 1);
        arbitre();
        gestion.lancerSuivant(123, new TirageEquitable());
        attendre(a);
        gestion.lancerSuivant(123, new TirageEquitable());
        attendre(b);
        for (int n = 0; n < 6; n++) {
            Match x = a.getMatchs().get(n), y = b.getMatchs().get(n);
            assertEquals(x.getVainqueur() == x.getEquipe1(), y.getVainqueur() == y.getEquipe1());
        }
    }

    @Test
    void limiteAtomiqueDeTroisEtAttenteBornee() throws Exception {
        List<Tournoi> liste = new ArrayList<>();
        for (int n = 0; n < 4; n++) {
            liste.add(tournoi("T" + n, 4, n));
            arbitre();
        }
        CountDownLatch liberer = new CountDownLatch(1);
        StrategieVictoire bloquee =
                (a, b, r) -> {
                    try {
                        if (!liberer.await(5, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Test bloqué");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(e);
                    }
                    return a;
                };
        ExecutorService demandes = Executors.newFixedThreadPool(4);
        CyclicBarrier barriere = new CyclicBarrier(4);
        List<Future<Boolean>> essais = new ArrayList<>();
        try {
            for (int n = 0; n < 4; n++) {
                essais.add(
                        demandes.submit(
                                () -> {
                                    barriere.await();
                                    try {
                                        gestion.lancerSuivant(1, bloquee);
                                        return true;
                                    } catch (IllegalStateException e) {
                                        return false;
                                    }
                                }));
            }
            int lances = 0;
            for (Future<Boolean> essai : essais) {
                if (essai.get(5, TimeUnit.SECONDS)) {
                    lances++;
                }
            }
            assertEquals(3, lances);
            assertEquals(3, gestion.getNombreEnCours());
            assertEquals(EtatTournoi.PLANIFIE, liste.get(3).getEtat());
            assertThrows(
                    TimeoutException.class,
                    () -> liste.get(0).attendreRapport(1, TimeUnit.MILLISECONDS));
            Equipe e = liste.get(0).getEquipes().get(0);
            assertThrows(IllegalStateException.class, () -> e.retirerJoueur(e.getJoueurs().get(0)));
            assertEquals(3, gestion.arbitres(true).size());
        } finally {
            liberer.countDown();
            demandes.shutdown();
        }
        for (int n = 0; n < 3; n++) {
            attendre(liste.get(n));
        }
        assertEquals(0, gestion.getNombreEnCours());
        assertEquals(3, depot.tous().size());
    }

    @Test
    void equipeNePeutPasJouerDeuxTournois() throws Exception {
        Tournoi t = tournoi("Premier", 4, 0);
        arbitre();
        arbitre();
        Tournoi autre = new Tournoi("Deuxième", jeu, t.getDate().plusDays(1), 200, 1);
        for (Equipe e : t.getEquipes()) {
            autre.inscrire(e);
        }
        gestion.planifier(autre);
        CountDownLatch fin = new CountDownLatch(1);
        try {
            gestion.lancerSuivant(
                    1,
                    (a, b, r) -> {
                        try {
                            if (!fin.await(5, TimeUnit.SECONDS)) {
                                throw new IllegalStateException("Délai test");
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(e);
                        }
                        return a;
                    });
            assertThrows(
                    IllegalStateException.class,
                    () -> gestion.lancerSuivant(2, new TirageEquitable()));
        } finally {
            fin.countDown();
        }
        attendre(t);
        gestion.lancerSuivant(2, new TirageEquitable());
        attendre(autre);
    }

    @Test
    void strategieInvalideLibereLesReservations() throws Exception {
        Tournoi t = tournoi("Erreur", 4, 0);
        Arbitre a = arbitre();
        gestion.lancerSuivant(1, (x, y, r) -> null);
        assertThrows(ExecutionException.class, () -> attendre(t));
        assertEquals(EtatTournoi.TERMINE, t.getEtat());
        assertEquals(0, gestion.getNombreEnCours());
        assertFalse(a.estOccupe());
        assertTrue(t.getRapport().isPresent());
    }

    @Test
    void fermetureAttendEtRefuseNouveauxLancements() {
        Tournoi t = tournoi("Fermeture", 4, 0);
        arbitre();
        gestion.lancerSuivant(1, new TirageEquitable());
        gestion.close();
        assertEquals(EtatTournoi.TERMINE, t.getEtat());
        assertEquals(0, gestion.getNombreEnCours());
        assertThrows(
                IllegalStateException.class, () -> gestion.lancerSuivant(2, new TirageEquitable()));
    }
}
