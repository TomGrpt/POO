package fr.imt.arena;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Registre des objets et responsable du lancement des tournois. */
public final class Arena implements AutoCloseable {
    private final Map<String, Personne> personnes = new LinkedHashMap<>();
    private final Map<String, Jeu> jeux = new HashMap<>();
    private final Map<String, Equipe> equipes = new HashMap<>();
    private final Map<String, Tournoi> tournois = new HashMap<>();
    private final PriorityQueue<Tournoi> planifies = new PriorityQueue<>();
    private final Set<Tournoi> enCours = new HashSet<>();
    private final List<EcouteurMatch> observateurs = new ArrayList<>();
    private final Queue<RuntimeException> erreursObservateurs = new ConcurrentLinkedQueue<>();
    private final ExecutorService executeur = Executors.newFixedThreadPool(3);
    private final DepotRapports depot;
    private boolean ferme;

    public Arena(DepotRapports depot) {
        this.depot = Objects.requireNonNull(depot);
    }

    private void verifierOuverture() {
        if (ferme) {
            throw new IllegalStateException("Arena est fermée");
        }
    }

    public synchronized void ajouter(Personne personne) {
        verifierOuverture();
        if (personnes.containsKey(personne.id())) {
            throw new IllegalArgumentException("Cet identifiant est déjà enregistré");
        }
        personnes.put(personne.id(), personne);
    }

    public synchronized void ajouter(Jeu jeu) {
        verifierOuverture();
        if (jeux.containsKey(jeu.nom())) {
            throw new IllegalArgumentException("Ce jeu est déjà enregistré");
        }
        jeux.put(jeu.nom(), jeu);
    }

    public synchronized void ajouter(Equipe equipe) {
        verifierOuverture();
        if (equipes.containsKey(equipe.nom())) {
            throw new IllegalArgumentException("Ce nom d'équipe est déjà enregistré");
        }
        equipes.put(equipe.nom(), equipe);
    }

    public synchronized void planifier(Tournoi tournoi) {
        verifierOuverture();
        if (tournoi.etat() != EtatTournoi.PLANIFIE || tournois.containsKey(tournoi.nom())) {
            throw new IllegalArgumentException("Ce tournoi ne peut pas être planifié");
        }
        tournois.put(tournoi.nom(), tournoi);
        planifies.add(tournoi);
    }

    public synchronized Optional<Equipe> equipe(String nom) {
        return Optional.ofNullable(equipes.get(nom));
    }

    public synchronized List<Tournoi> planifies() {
        List<Tournoi> resultat = new ArrayList<>(planifies);
        resultat.sort(null); // Utiliser l'ordre défini par Tournoi.compareTo.
        return List.copyOf(resultat);
    }

    public synchronized List<Tournoi> termines() {
        List<Tournoi> resultat = new ArrayList<>();
        for (Tournoi tournoi : tournois.values()) {
            if (tournoi.etat() == EtatTournoi.TERMINE) {
                resultat.add(tournoi);
            }
        }
        resultat.sort(new Comparator<Tournoi>() {
            @Override
            public int compare(Tournoi premier, Tournoi second) {
                int ordre = second.budget().compareTo(premier.budget());
                if (ordre == 0) {
                    ordre = premier.nom().compareTo(second.nom());
                }
                return ordre;
            }
        });
        return List.copyOf(resultat);
    }

    public synchronized List<Equipe> classement() {
        // Figer les scores avant le tri : ils peuvent évoluer pendant la simulation.
        Map<Equipe, Integer> scores = new HashMap<>();
        for (Equipe equipe : equipes.values()) {
            scores.put(equipe, equipe.points());
        }
        List<Equipe> resultat = new ArrayList<>(equipes.values());
        resultat.sort(new Comparator<Equipe>() {
            @Override
            public int compare(Equipe premiere, Equipe seconde) {
                int ordre = Integer.compare(scores.get(seconde), scores.get(premiere));
                if (ordre == 0) {
                    ordre = premiere.nom().compareTo(seconde.nom());
                }
                return ordre;
            }
        });
        return List.copyOf(resultat);
    }

    public synchronized List<Arbitre> arbitres(boolean disponibles) {
        List<Arbitre> resultat = new ArrayList<>();
        for (Personne personne : personnes.values()) {
            if (personne instanceof Arbitre) {
                Arbitre arbitre = (Arbitre) personne;
                if (arbitre.disponible() == disponibles) {
                    resultat.add(arbitre);
                }
            }
        }
        return List.copyOf(resultat);
    }

    public synchronized List<Match> matchs(Equipe equipe) {
        List<Tournoi> listeTournois = new ArrayList<>(tournois.values());
        listeTournois.sort(null);
        List<Match> resultat = new ArrayList<>();
        for (Tournoi tournoi : listeTournois) {
            for (Match match : tournoi.matchs()) {
                if (match.concerne(equipe)) {
                    resultat.add(match);
                }
            }
        }
        return List.copyOf(resultat);
    }

    public synchronized int nombreEnCours() {
        return enCours.size();
    }

    public synchronized void observer(EcouteurMatch observateur) {
        observateurs.add(Objects.requireNonNull(observateur));
    }

    public List<RuntimeException> erreursObservateurs() {
        return List.copyOf(erreursObservateurs);
    }

    private List<Arbitre> chercherArbitresQualifies(Jeu jeu) {
        List<Arbitre> resultat = new ArrayList<>();
        for (Personne personne : personnes.values()) {
            if (personne instanceof Arbitre) {
                Arbitre arbitre = (Arbitre) personne;
                if (arbitre.estQualifie(jeu)) {
                    resultat.add(arbitre);
                }
            }
        }
        return resultat;
    }

    public synchronized Tournoi lancerSuivant(ParametresSimulation parametres, long graine) {
        verifierOuverture();
        Objects.requireNonNull(parametres);
        if (enCours.size() >= 3) {
            throw new IllegalStateException("Trois tournois sont déjà en cours");
        }
        Tournoi tournoi = planifies.peek();
        if (tournoi == null) {
            throw new IllegalStateException("Aucun tournoi n'est planifié");
        }
        List<Arbitre> qualifies = chercherArbitresQualifies(tournoi.jeu());
        int nombreEquipes = tournoi.equipes().size();
        int nombreMatchs = Math.multiplyExact(nombreEquipes, nombreEquipes - 1) / 2;
        parametres.dureeNanos(nombreMatchs, tournoi.arenes());

        tournoi.demarrer(qualifies);
        planifies.remove();
        enCours.add(tournoi);
        for (Arbitre arbitre : qualifies) {
            arbitre.engager();
        }
        List<EcouteurMatch> abonnes = List.copyOf(observateurs);
        // Cette petite lambda confie le travail au pool et laisse l'appelant continuer.
        executeur.execute(() -> simuler(tournoi, qualifies, parametres, graine, abonnes));
        return tournoi;
    }

    private void simuler(Tournoi tournoi, List<Arbitre> arbitres,
                         ParametresSimulation parametres, long graine,
                         List<EcouteurMatch> abonnes) {
        Throwable erreurRencontree = null;
        Random random = new Random(graine);
        try {
            Simulateur.executer(tournoi, parametres, random, abonnes, erreursObservateurs);
        } catch (InterruptedException erreur) {
            Thread.currentThread().interrupt();
            erreurRencontree = erreur;
        } catch (RuntimeException erreur) {
            erreurRencontree = erreur;
        }

        Rapport rapport = null;
        try {
            // Une interruption ne doit pas laisser des rencontres ou des équipes bloquées.
            for (Match match : tournoi.matchs()) {
                if (match.etat() == EtatMatch.PREVU) {
                    tournoi.terminerMatch(match, true, parametres.strategie(), random);
                }
            }
            rapport = tournoi.cloturer();
            depot.sauvegarder(rapport);
        } catch (RuntimeException erreur) {
            if (erreurRencontree != null) {
                erreur.addSuppressed(erreurRencontree);
            }
            erreurRencontree = erreur;
        } finally {
            libererEngagements(tournoi, arbitres);
        }
        if (erreurRencontree == null) {
            tournoi.publier(rapport);
        } else {
            tournoi.echec(erreurRencontree);
        }
    }

    private synchronized void libererEngagements(Tournoi tournoi, List<Arbitre> arbitres) {
        for (Arbitre arbitre : arbitres) {
            arbitre.liberer();
        }
        enCours.remove(tournoi);
    }

    @Override
    public void close() {
        synchronized (this) {
            if (ferme) {
                return;
            }
            ferme = true;
            executeur.shutdown();
        }
        // Ne pas garder le verrou d'Arena pendant l'attente des workers.
        boolean interrompu = false;
        try {
            if (!executeur.awaitTermination(30, TimeUnit.SECONDS)) {
                executeur.shutdownNow();
                if (!executeur.awaitTermination(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Une tâche refuse de s'arrêter");
                }
            }
        } catch (InterruptedException erreur) {
            interrompu = true;
            executeur.shutdownNow();
            while (!executeur.isTerminated()) {
                try {
                    if (!executeur.awaitTermination(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Arrêt impossible", erreur);
                    }
                } catch (InterruptedException nouvelleInterruption) {
                    interrompu = true;
                }
            }
        } finally {
            if (interrompu) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
