package fr.imt.arena.metier;

import fr.imt.arena.jeux.Jeu;
import fr.imt.arena.persistance.RapportTournoi;
import fr.imt.arena.util.Textes;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

// État du tournoi
public final class Tournoi implements Comparable<Tournoi> {
    private final String nom;
    private final Jeu jeu;
    private final LocalDate date;
    private final double budget;
    private final int arenes;
    private final List<Equipe> equipes = new ArrayList<>();
    private EtatTournoi etat = EtatTournoi.PLANIFIE;
    private List<Match> matchs = List.of();
    private RapportTournoi rapport;
    private Future<RapportTournoi> resultat;
    private boolean enregistre;

    public Tournoi(String nom, Jeu jeu, LocalDate date, double budget, int arenes) {
        if (!Double.isFinite(budget) || budget < 0 || arenes < 1 || arenes > 4) {
            throw new IllegalArgumentException("Budget positif ou nul, 1 à 4 arènes");
        }
        this.nom = Textes.verifier(nom);
        this.jeu = Objects.requireNonNull(jeu);
        this.date = Objects.requireNonNull(date);
        this.budget = budget;
        this.arenes = arenes;
    }

    // Inscription des équipes
    public void inscrire(Equipe equipe) {
        synchronized (Verrou.OBJET) {
            if (etat != EtatTournoi.PLANIFIE) {
                throw new IllegalStateException("Inscriptions fermées");
            }
            Objects.requireNonNull(equipe);
            if (equipes.contains(equipe)) {
                throw new IllegalArgumentException("Equipe déjà inscrite");
            }
            equipes.add(equipe);
        }
    }

    public String getNom() {
        return nom;
    }

    public Jeu getJeu() {
        return jeu;
    }

    public LocalDate getDate() {
        return date;
    }

    public double getBudget() {
        return budget;
    }

    public int getArenes() {
        return arenes;
    }

    public List<Equipe> getEquipes() {
        synchronized (Verrou.OBJET) {
            return List.copyOf(equipes);
        }
    }

    public int getNombreMatchs() {
        synchronized (Verrou.OBJET) {
            return equipes.size() * (equipes.size() - 1) / 2;
        }
    }

    public EtatTournoi getEtat() {
        synchronized (Verrou.OBJET) {
            return etat;
        }
    }

    public List<Match> getMatchs() {
        synchronized (Verrou.OBJET) {
            return matchs;
        }
    }

    public Optional<RapportTournoi> getRapport() {
        synchronized (Verrou.OBJET) {
            return Optional.ofNullable(rapport);
        }
    }

    // Regroupement des victoires
    public Map<Equipe, List<Match>> matchsParVainqueur() {
        synchronized (Verrou.OBJET) {
            Map<Equipe, List<Match>> groupes = new LinkedHashMap<>();
            for (Match match : matchs) {
                if (match.estJoue()) {
                    Equipe vainqueur = match.getVainqueur();
                    if (!groupes.containsKey(vainqueur)) {
                        groupes.put(vainqueur, new ArrayList<>());
                    }
                    groupes.get(vainqueur).add(match);
                }
            }
            for (Equipe equipe : groupes.keySet()) {
                groupes.put(equipe, List.copyOf(groupes.get(equipe)));
            }
            return Collections.unmodifiableMap(groupes);
        }
    }

    // Attente hors verrou
    public RapportTournoi attendreRapport(long delai, TimeUnit unite)
            throws InterruptedException, ExecutionException, TimeoutException {
        Future<RapportTournoi> copie;
        synchronized (Verrou.OBJET) {
            if (resultat == null) {
                throw new IllegalStateException("Tournoi pas encore lancé");
            }
            copie = resultat;
        }

        return copie.get(delai, unite);
    }

    void enregistrer() {
        if (enregistre) {
            throw new IllegalStateException("Tournoi déjà planifié");
        }
        enregistre = true;
    }

    void commencer(Future<RapportTournoi> resultat) {
        this.resultat = resultat;
        etat = EtatTournoi.EN_COURS;
    }

    // Publication des résultats
    void terminer(List<Match> matchs, RapportTournoi rapport) {
        this.matchs = List.copyOf(matchs);
        this.rapport = rapport;
        etat = EtatTournoi.TERMINE;
    }

    @Override
    // Priorité chronologique
    public int compareTo(Tournoi autre) {
        int comparaison = date.compareTo(autre.date);
        if (comparaison == 0) {
            comparaison = nom.compareTo(autre.nom);
        }
        return comparaison;
    }

    @Override
    public String toString() {
        return nom + " (" + date + ")";
    }
}
