package fr.imt.arena.metier;

import fr.imt.arena.jeux.Jeu;
import fr.imt.arena.persistance.DepotRapports;
import fr.imt.arena.persistance.RapportTournoi;
import fr.imt.arena.strategies.StrategieVictoire;

import java.util.*;
import java.util.concurrent.*;

// Gestion des tournois
public final class GestionnaireTournois implements AutoCloseable {
    public static final long TEMPS_PAR_MATCH_MS = 100;
    private final Map<String, Equipe> equipes = new HashMap<>();
    private final Map<Integer, Personne> personnes = new HashMap<>();
    private final Map<String, Jeu> jeux = new HashMap<>();
    private final PriorityQueue<Tournoi> planifies = new PriorityQueue<>();
    private final List<Tournoi> tournois = new ArrayList<>();
    private final ExecutorService pool = Executors.newFixedThreadPool(3);
    private final DepotRapports depot;
    private final long tempsMatch;
    private final double risqueIncident;
    private int enCours;
    private boolean ferme;

    public GestionnaireTournois(DepotRapports depot) {
        this(depot, TEMPS_PAR_MATCH_MS, 0.10);
    }

    public GestionnaireTournois(DepotRapports depot, long tempsMatch, double risqueIncident) {
        if (tempsMatch < 0
                || !Double.isFinite(risqueIncident)
                || risqueIncident < 0
                || risqueIncident > 1) {
            throw new IllegalArgumentException("Temps >= 0, probabilité entre 0 et 1");
        }
        this.depot = Objects.requireNonNull(depot);
        this.tempsMatch = tempsMatch;
        this.risqueIncident = risqueIncident;
    }

    private void verifierOuvert() {
        if (ferme) {
            throw new IllegalStateException("Gestionnaire fermé");
        }
    }

    public void ajouterPersonne(Personne personne) {
        synchronized (Verrou.OBJET) {
            verifierOuvert();
            if (personnes.containsKey(personne.getId())) {
                throw new IllegalArgumentException("Id déjà présent");
            }
            personnes.put(personne.getId(), personne);
        }
    }

    public void ajouterJeu(Jeu jeu) {
        synchronized (Verrou.OBJET) {
            verifierOuvert();
            if (jeux.containsKey(jeu.getNom())) {
                throw new IllegalArgumentException("Nom de jeu déjà présent");
            }
            jeux.put(jeu.getNom(), jeu);
        }
    }

    public void ajouterEquipe(Equipe equipe) {
        synchronized (Verrou.OBJET) {
            verifierOuvert();
            if (equipes.containsKey(equipe.getNom())) {
                throw new IllegalArgumentException("Nom d'équipe déjà présent");
            }
            equipes.put(equipe.getNom(), equipe);
        }
    }

    public Equipe equipeParNom(String nom) {
        synchronized (Verrou.OBJET) {
            return equipes.get(nom);
        }
    }

    // Ajout au calendrier
    public void planifier(Tournoi tournoi) {
        synchronized (Verrou.OBJET) {
            verifierOuvert();
            if (jeux.get(tournoi.getJeu().getNom()) != tournoi.getJeu()) {
                throw new IllegalArgumentException("Jeu non enregistré");
            }
            for (Tournoi autre : tournois) {
                if (autre.getNom().equals(tournoi.getNom())) {
                    throw new IllegalArgumentException("Nom de tournoi déjà présent");
                }
            }
            tournoi.enregistrer();
            planifies.add(tournoi);
            tournois.add(tournoi);
        }
    }

    public List<Tournoi> tournoisPlanifies() {
        synchronized (Verrou.OBJET) {
            List<Tournoi> copie = new ArrayList<>(planifies);
            Collections.sort(copie);
            return List.copyOf(copie);
        }
    }

    public List<Tournoi> tournoisTermines() {
        synchronized (Verrou.OBJET) {
            List<Tournoi> liste = new ArrayList<>();
            for (Tournoi tournoi : tournois) {
                if (tournoi.getEtat() == EtatTournoi.TERMINE) {
                    liste.add(tournoi);
                }
            }
            liste.sort(
                    new Comparator<Tournoi>() {
                        @Override
                        public int compare(Tournoi premier, Tournoi second) {
                            int ordre = Double.compare(second.getBudget(), premier.getBudget());
                            if (ordre == 0) {
                                ordre = premier.getNom().compareTo(second.getNom());
                            }
                            return ordre;
                        }
                    });
            return List.copyOf(liste);
        }
    }

    // Tri des points
    public List<Equipe> classement() {
        synchronized (Verrou.OBJET) {
            List<Equipe> liste = new ArrayList<>(equipes.values());
            liste.sort(
                    new Comparator<Equipe>() {
                        @Override
                        public int compare(Equipe premiere, Equipe seconde) {
                            int ordre = Integer.compare(seconde.getPoints(), premiere.getPoints());
                            if (ordre == 0) {
                                ordre = premiere.getNom().compareTo(seconde.getNom());
                            }
                            return ordre;
                        }
                    });
            return List.copyOf(liste);
        }
    }

    public List<Arbitre> arbitres(boolean occupes) {
        synchronized (Verrou.OBJET) {
            List<Arbitre> liste = new ArrayList<>();
            for (Personne personne : personnes.values()) {
                if (personne instanceof Arbitre arbitre && arbitre.estOccupe() == occupes) {
                    liste.add(arbitre);
                }
            }
            liste.sort(
                    new Comparator<Arbitre>() {
                        @Override
                        public int compare(Arbitre premier, Arbitre second) {
                            return Integer.compare(premier.getId(), second.getId());
                        }
                    });
            return List.copyOf(liste);
        }
    }

    public List<Match> matchsEquipe(Equipe equipe) {
        synchronized (Verrou.OBJET) {
            List<Match> liste = new ArrayList<>();
            for (Tournoi tournoi : tournois) {
                for (Match match : tournoi.getMatchs()) {
                    if (match.getEquipe1() == equipe || match.getEquipe2() == equipe) {
                        liste.add(match);
                    }
                }
            }
            return List.copyOf(liste);
        }
    }

    public int getNombreEnCours() {
        synchronized (Verrou.OBJET) {
            return enCours;
        }
    }

    // Vérification et réservation
    public Tournoi lancerSuivant(long graine, StrategieVictoire strategie) {
        synchronized (Verrou.OBJET) {
            verifierOuvert();
            Objects.requireNonNull(strategie);
            if (enCours >= 3) {
                throw new IllegalStateException("Déjà trois tournois en cours");
            }
            Tournoi tournoi = planifies.peek();
            if (tournoi == null) {
                throw new IllegalStateException("Aucun tournoi planifié");
            }
            if (tournoi.getEquipes().size() < 4) {
                throw new IllegalStateException("Au moins quatre équipes");
            }
            for (Equipe equipe : tournoi.getEquipes()) {
                if (equipes.get(equipe.getNom()) != equipe) {
                    throw new IllegalStateException("Equipe non enregistrée");
                }
                if (equipe.estOccupee() || equipe.getCoach() == null) {
                    throw new IllegalStateException("Equipe occupée ou sans coach : " + equipe);
                }
                if (personnes.get(equipe.getCoach().getId()) != equipe.getCoach()) {
                    throw new IllegalStateException("Coach non enregistré");
                }
                tournoi.getJeu().selectionner(equipe);
                for (Joueur joueur : equipe.getJoueurs()) {
                    if (personnes.get(joueur.getId()) != joueur) {
                        throw new IllegalStateException("Joueur non enregistré");
                    }
                }
            }

            Arbitre choisi = null;
            for (Arbitre arbitre : arbitres(false)) {
                if (arbitre.estQualifie(tournoi.getJeu())) {
                    choisi = arbitre;
                    break;
                }
            }
            if (choisi == null) {
                throw new IllegalStateException("Aucun arbitre qualifié disponible");
            }
            Arbitre arbitre = choisi;
            // Tâche exécutée séparément
            FutureTask<RapportTournoi> travail =
                    new FutureTask<>(() -> simuler(tournoi, arbitre, graine, strategie));

            for (Equipe equipe : tournoi.getEquipes()) {
                equipe.reserver(true);
            }
            arbitre.reserver(true);
            enCours++;
            planifies.remove();
            tournoi.commencer(travail);
            pool.execute(travail);
            return tournoi;
        }
    }

    // Simulation des rencontres
    private RapportTournoi simuler(
            Tournoi tournoi, Arbitre arbitre, long graine, StrategieVictoire strategie) {
        Random hasard = new Random(graine);
        List<Match> resultats = new ArrayList<>();
        List<Equipe> participants = tournoi.getEquipes();
        RuntimeException erreur = null;
        boolean interrompu = false;

        // Cadence des matchs
        long pasNanos = TimeUnit.MILLISECONDS.toNanos(tempsMatch) / tournoi.getArenes();
        long debut = System.nanoTime();
        int numero = 0;
        for (int i = 0; i < participants.size(); i++) {
            for (int j = i + 1; j < participants.size(); j++) {
                Equipe premiere = participants.get(i);
                Equipe seconde = participants.get(j);
                Equipe gagnant = null;
                numero++;
                if (!interrompu && erreur == null) {
                    try {
                        long reste = numero * pasNanos - (System.nanoTime() - debut);
                        if (reste > 0) {
                            TimeUnit.NANOSECONDS.sleep(reste);
                        }
                        if (Thread.currentThread().isInterrupted()) {
                            throw new InterruptedException();
                        }
                        if (hasard.nextDouble() >= risqueIncident) {
                            gagnant = strategie.choisir(premiere, seconde, hasard);
                            if (gagnant != premiere && gagnant != seconde) {
                                throw new IllegalStateException(
                                        "La stratégie doit choisir une des deux équipes");
                            }
                        }
                    } catch (InterruptedException e) {
                        interrompu = true;
                    } catch (RuntimeException e) {
                        erreur = e;
                        gagnant = null;
                    }
                }
                resultats.add(new Match(premiere, seconde, tournoi.getJeu(), arbitre, gagnant));
            }
        }
        RapportTournoi rapport;
        // Bilan sous verrou
        synchronized (Verrou.OBJET) {
            Map<Equipe, Integer> pointsLocaux = new HashMap<>();
            for (Equipe equipe : participants) {
                pointsLocaux.put(equipe, 0);
            }
            int joues = 0;
            for (Match match : resultats) {
                if (match.estJoue()) {
                    joues++;
                    Equipe gagnant = match.getVainqueur();
                    gagnant.gagner();
                    arbitre.compterMatch();
                    pointsLocaux.put(gagnant, pointsLocaux.get(gagnant) + 3);
                }
            }
            participants = new ArrayList<>(participants);
            participants.sort(
                    new Comparator<Equipe>() {
                        @Override
                        public int compare(Equipe premiere, Equipe seconde) {
                            int ordre =
                                    Integer.compare(
                                            pointsLocaux.get(seconde), pointsLocaux.get(premiere));
                            if (ordre == 0) {
                                ordre = premiere.getNom().compareTo(seconde.getNom());
                            }
                            return ordre;
                        }
                    });
            String vainqueur = "Aucun";
            if (joues > 0) {
                vainqueur = participants.get(0).getNom();
            }
            rapport = new RapportTournoi(tournoi, joues, vainqueur);
            try {
                depot.enregistrer(rapport);
            } catch (RuntimeException e) {
                if (erreur == null) {
                    erreur = e;
                } else {
                    erreur.addSuppressed(e);
                }
            } finally {

                tournoi.terminer(resultats, rapport);
                for (Equipe equipe : participants) {
                    equipe.reserver(false);
                }
                arbitre.reserver(false);
                enCours--;
            }
        }
        if (interrompu) {
            Thread.currentThread().interrupt();
        }
        if (erreur != null) {
            throw erreur;
        }
        return rapport;
    }

    @Override
    // Arrêt des threads
    public void close() {
        synchronized (Verrou.OBJET) {
            ferme = true;
            pool.shutdown();
        }
        boolean interrompu = false;

        while (!pool.isTerminated()) {
            try {
                pool.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                interrompu = true;
                pool.shutdownNow();
            }
        }
        if (interrompu) {
            Thread.currentThread().interrupt();
        }
    }
}
