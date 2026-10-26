package fr.imt.arena;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/** Service applicatif : registre et ordonnancement. Aucun accès lent sous son moniteur. */
public final class Arena implements AutoCloseable {
    private final Map<String, Personne> personnes = new LinkedHashMap<>();
    private final Map<String, Jeu> jeux = new HashMap<>();
    private final Map<String, Equipe> equipes = new HashMap<>();
    private final Map<String, Tournoi> tournois = new HashMap<>();
    private final PriorityQueue<Tournoi> planifies = new PriorityQueue<>();
    private final Set<Tournoi> enCours = new HashSet<>();
    private final List<EcouteurMatch> observateurs = new CopyOnWriteArrayList<>();
    private final Queue<RuntimeException> erreursObservateurs = new ConcurrentLinkedQueue<>();
    private final ExecutorService executeur = Executors.newFixedThreadPool(3);
    private final DepotRapports depot;
    private boolean ferme;
    public Arena(DepotRapports depot) { this.depot = Objects.requireNonNull(depot); }
    private void ouverte() { if (ferme) throw new IllegalStateException("Arena fermée"); }
    public synchronized void ajouter(Personne p) {
        ouverte(); if (personnes.putIfAbsent(p.id(), p) != null) throw new IllegalArgumentException("Id déjà enregistré");
    }
    public synchronized void ajouter(Jeu j) {
        ouverte(); if (jeux.putIfAbsent(j.nom(), j) != null) throw new IllegalArgumentException("Jeu déjà enregistré");
    }
    public synchronized void ajouter(Equipe e) {
        ouverte(); if (equipes.putIfAbsent(e.nom(), e) != null) throw new IllegalArgumentException("Nom déjà enregistré");
    }
    public synchronized void planifier(Tournoi t) {
        ouverte();
        if (t.etat() != EtatTournoi.PLANIFIE || tournois.putIfAbsent(t.nom(), t) != null) throw new IllegalArgumentException("Tournoi non planifiable");
        planifies.add(t);
    }
    public synchronized Optional<Equipe> equipe(String nom) { return Optional.ofNullable(equipes.get(nom)); }
    public synchronized List<Tournoi> planifies() { return planifies.stream().sorted().toList(); }
    public synchronized List<Tournoi> termines() {
        return tournois.values().stream().filter(t -> t.etat() == EtatTournoi.TERMINE)
            .sorted(Comparator.comparing(Tournoi::budget).reversed().thenComparing(Tournoi::nom)).toList();
    }
    public synchronized List<Equipe> classement() {
        // Capturer les points une seule fois évite un comparateur instable pendant une simulation.
        Map<Equipe, Integer> instantane = new HashMap<>(); equipes.values().forEach(e -> instantane.put(e, e.points()));
        return equipes.values().stream().sorted(Comparator.<Equipe>comparingInt(instantane::get).reversed().thenComparing(Equipe::nom)).toList();
    }
    public synchronized List<Arbitre> arbitres(boolean disponibles) {
        return personnes.values().stream().filter(Arbitre.class::isInstance).map(Arbitre.class::cast)
            .filter(a -> a.disponible() == disponibles).toList();
    }
    public synchronized List<Match> matchs(Equipe equipe) {
        return tournois.values().stream().sorted().flatMap(t -> t.matchs().stream()).filter(m -> m.concerne(equipe)).toList();
    }
    public synchronized int nombreEnCours() { return enCours.size(); }
    public void observer(EcouteurMatch observateur) { observateurs.add(Objects.requireNonNull(observateur)); }
    public List<RuntimeException> erreursObservateurs() { return List.copyOf(erreursObservateurs); }
    public synchronized Tournoi lancerSuivant(ParametresSimulation parametres, long graine) {
        ouverte(); Objects.requireNonNull(parametres);
        if (enCours.size() >= 3) throw new IllegalStateException("Trois tournois déjà en cours");
        Tournoi tournoi = planifies.peek();
        if (tournoi == null) throw new IllegalStateException("Aucun tournoi planifié");
        List<Arbitre> qualifies = personnes.values().stream().filter(Arbitre.class::isInstance)
            .map(Arbitre.class::cast).filter(a -> a.estQualifie(tournoi.jeu())).toList();
        // Valider le calcul avant toute réservation.
        int n = tournoi.equipes().size(); parametres.dureeNanos(Math.multiplyExact(n, n - 1) / 2, tournoi.arenes());
        tournoi.demarrer(qualifies);
        planifies.remove(); enCours.add(tournoi); qualifies.forEach(Arbitre::engager);
        List<EcouteurMatch> abonnes = List.copyOf(observateurs);
        executeur.execute(() -> simuler(tournoi, qualifies, parametres, graine, abonnes));
        return tournoi;
    }
    private void simuler(Tournoi tournoi, List<Arbitre> arbitres, ParametresSimulation parametres, long graine, List<EcouteurMatch> abonnes) {
        Throwable echec = null;
        try { Simulateur.executer(tournoi, parametres, new Random(graine), abonnes, erreursObservateurs::add); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); echec = e; }
        catch (RuntimeException e) { echec = e; }
        // Même en cas d'interruption/stratégie défaillante, annuler les rencontres restantes et libérer les effectifs.
        Rapport rapport;
        try {
            for (Match m : tournoi.matchs()) if (m.etat() == EtatMatch.PREVU)
                tournoi.terminerMatch(m, true, parametres.strategie(), new Random(0));
            rapport = tournoi.cloturer();
            depot.sauvegarder(rapport);
        } catch (RuntimeException e) {
            if (echec != null) e.addSuppressed(echec);
            echec = e; rapport = null;
        } finally {
            synchronized (this) { arbitres.forEach(Arbitre::liberer); enCours.remove(tournoi); }
        }
        if (echec == null) tournoi.publier(rapport); else tournoi.echec(echec);
    }
    @Override public void close() {
        synchronized (this) { if (ferme) return; ferme = true; executeur.shutdown(); }
        boolean interrompu = false;
        try {
            if (!executeur.awaitTermination(30, TimeUnit.SECONDS)) {
                executeur.shutdownNow();
                if (!executeur.awaitTermination(10, TimeUnit.SECONDS)) throw new IllegalStateException("Simulation non coopérative");
            }
        } catch (InterruptedException e) {
            interrompu = true; executeur.shutdownNow();
            // Attendre réellement les workers avant que le propriétaire ferme le dépôt.
            while (!executeur.isTerminated()) {
                try { if (!executeur.awaitTermination(10, TimeUnit.SECONDS)) throw new IllegalStateException("Arrêt impossible", e); }
                catch (InterruptedException ignore) { interrompu = true; }
            }
        } finally { if (interrompu) Thread.currentThread().interrupt(); }
    }
}
