package fr.imt.arena;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class Tournoi implements Comparable<Tournoi> {
    private final String nom;
    private final Jeu jeu;
    private final LocalDateTime date;
    private final BigDecimal budget;
    private final int arenes;
    private final List<Equipe> equipes = new ArrayList<>();
    private final List<Match> matchs = new ArrayList<>();
    private final Map<Equipe, Integer> points = new HashMap<>();
    private final CompletableFuture<Rapport> rapport = new CompletableFuture<>();
    private EtatTournoi etat = EtatTournoi.PLANIFIE;

    public Tournoi(String nom, Jeu jeu, LocalDateTime date, BigDecimal budget, int arenes) {
        this.nom = Verifications.texte(nom);
        this.jeu = Objects.requireNonNull(jeu);
        this.date = Objects.requireNonNull(date);
        this.budget = Objects.requireNonNull(budget);
        if (budget.signum() < 0) {
            throw new IllegalArgumentException("Le budget ne peut pas être négatif");
        }
        if (arenes < 1 || arenes > 4) {
            throw new IllegalArgumentException("Il faut entre 1 et 4 arènes");
        }
        this.arenes = arenes;
    }

    public String nom() {
        return nom;
    }

    public Jeu jeu() {
        return jeu;
    }

    public LocalDateTime date() {
        return date;
    }

    public BigDecimal budget() {
        return budget;
    }

    public int arenes() {
        return arenes;
    }

    public synchronized EtatTournoi etat() {
        return etat;
    }

    public synchronized List<Equipe> equipes() {
        return List.copyOf(equipes);
    }

    public synchronized List<Match> matchs() {
        return List.copyOf(matchs);
    }

    public synchronized void inscrire(Equipe equipe) {
        if (etat != EtatTournoi.PLANIFIE) {
            throw new IllegalStateException("Les inscriptions sont fermées");
        }
        Objects.requireNonNull(equipe);
        for (Equipe inscrite : equipes) {
            if (inscrite.nom().equals(equipe.nom())) {
                throw new IllegalArgumentException("Ce nom d'équipe est déjà inscrit");
            }
        }
        equipes.add(equipe);
    }

    synchronized void demarrer(List<Arbitre> arbitres) {
        if (etat != EtatTournoi.PLANIFIE) {
            throw new IllegalStateException("Le tournoi a déjà été lancé");
        }
        if (equipes.size() < 4) {
            throw new IllegalStateException("Il faut au moins quatre équipes");
        }
        if (arbitres.isEmpty()) {
            throw new IllegalStateException("Il faut un arbitre qualifié");
        }

        List<Equipe> reservees = new ArrayList<>();
        try {
            for (Equipe equipe : equipes) {
                equipe.reserver(this, jeu);
                reservees.add(equipe);
            }
            List<Match> rencontres = creerRencontres(arbitres);
            matchs.addAll(rencontres);
            for (Equipe equipe : equipes) {
                points.put(equipe, 0);
            }
            etat = EtatTournoi.EN_COURS;
        } catch (RuntimeException erreur) {
            // Si une équipe n'est pas prête, ne pas bloquer les précédentes.
            for (Equipe equipe : reservees) {
                equipe.liberer(this);
            }
            throw erreur;
        }
    }

    private List<Match> creerRencontres(List<Arbitre> arbitres) {
        List<Match> rencontres = new ArrayList<>();
        for (int i = 0; i < equipes.size(); i++) {
            for (int j = i + 1; j < equipes.size(); j++) {
                // Le modulo fait revenir au premier arbitre après le dernier.
                int numeroArbitre = rencontres.size() % arbitres.size();
                Arbitre arbitre = arbitres.get(numeroArbitre);
                Match match = new Match(equipes.get(i), equipes.get(j), jeu, arbitre);
                rencontres.add(match);
            }
        }
        return rencontres;
    }

    synchronized void terminerMatch(Match match, boolean incident,
                                     StrategieVainqueur strategie, Random random) {
        if (etat != EtatTournoi.EN_COURS || !matchs.contains(match)) {
            throw new IllegalStateException("Ce match ne peut pas être joué ici");
        }
        match.terminer(incident, strategie, random);
        if (match.vainqueur().isPresent()) {
            Equipe gagnante = match.vainqueur().get();
            int anciensPoints = points.get(gagnante);
            points.put(gagnante, anciensPoints + 3);
        }
    }

    public synchronized Map<Equipe, Integer> pointsTournoi() {
        return Map.copyOf(points);
    }

    public synchronized List<Equipe> classement() {
        List<Equipe> resultat = new ArrayList<>(equipes);
        resultat.sort(new Comparator<Equipe>() {
            @Override
            public int compare(Equipe premiere, Equipe seconde) {
                int pointsPremiere = points.getOrDefault(premiere, 0);
                int pointsSeconde = points.getOrDefault(seconde, 0);
                int ordre = Integer.compare(pointsSeconde, pointsPremiere);
                if (ordre == 0) {
                    ordre = premiere.nom().compareTo(seconde.nom());
                }
                return ordre;
            }
        });
        return List.copyOf(resultat);
    }

    public synchronized Map<Equipe, List<Match>> matchsParVainqueur() {
        Map<Equipe, List<Match>> groupes = new LinkedHashMap<>();
        for (Equipe equipe : classement()) {
            List<Match> victoires = new ArrayList<>();
            for (Match match : matchs) {
                if (match.vainqueur().orElse(null) == equipe) {
                    victoires.add(match);
                }
            }
            if (!victoires.isEmpty()) {
                groupes.put(equipe, List.copyOf(victoires));
            }
        }
        return Collections.unmodifiableMap(groupes);
    }

    public synchronized VueTournoi consulter() {
        Map<String, Integer> copiePoints = new LinkedHashMap<>();
        for (Equipe equipe : equipes) {
            copiePoints.put(equipe.nom(), points.getOrDefault(equipe, 0));
        }
        List<ResultatMatch> copieMatchs = new ArrayList<>();
        for (Match match : matchs) {
            String nomVainqueur = null;
            if (match.vainqueur().isPresent()) {
                nomVainqueur = match.vainqueur().get().nom();
            }
            ResultatMatch copie = new ResultatMatch(match.premiere().nom(),
                match.seconde().nom(), match.etat(), nomVainqueur);
            copieMatchs.add(copie);
        }
        return new VueTournoi(etat, copiePoints, copieMatchs);
    }

    synchronized Rapport cloturer() {
        int nombreJoues = 0;
        for (Match match : matchs) {
            if (match.etat() == EtatMatch.JOUE) {
                nombreJoues++;
            }
        }
        String nomVainqueur = null;
        if (nombreJoues > 0) {
            nomVainqueur = classement().get(0).nom();
        }
        etat = EtatTournoi.TERMINE;
        for (Equipe equipe : equipes) {
            equipe.liberer(this);
        }
        return new Rapport(nom, date, jeu.nom(), matchs.size(), nombreJoues, nomVainqueur);
    }

    void publier(Rapport resultat) {
        rapport.complete(resultat);
    }

    void echec(Throwable cause) {
        rapport.completeExceptionally(cause);
    }

    public Rapport attendreRapport(Duration delai)
            throws InterruptedException, ExecutionException, TimeoutException {
        if (delai.isNegative()) {
            throw new IllegalArgumentException("Le délai ne peut pas être négatif");
        }
        return rapport.get(delai.toNanos(), TimeUnit.NANOSECONDS);
    }

    @Override
    public int compareTo(Tournoi autre) {
        int ordre = date.compareTo(autre.date);
        if (ordre == 0) {
            ordre = nom.compareTo(autre.nom);
        }
        return ordre;
    }
}
