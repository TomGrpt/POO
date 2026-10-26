package fr.imt.arena;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

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
        this.nom = Verifications.texte(nom); this.jeu = Objects.requireNonNull(jeu);
        this.date = Objects.requireNonNull(date); this.budget = Objects.requireNonNull(budget);
        if (budget.signum() < 0 || arenes < 1 || arenes > 4) throw new IllegalArgumentException("Budget ou arènes invalide");
        this.arenes = arenes;
    }
    public String nom() { return nom; }
    public Jeu jeu() { return jeu; }
    public LocalDateTime date() { return date; }
    public BigDecimal budget() { return budget; }
    public int arenes() { return arenes; }
    public synchronized EtatTournoi etat() { return etat; }
    public synchronized List<Equipe> equipes() { return List.copyOf(equipes); }
    public synchronized List<Match> matchs() { return List.copyOf(matchs); }
    public synchronized void inscrire(Equipe equipe) {
        if (etat != EtatTournoi.PLANIFIE) throw new IllegalStateException("Inscriptions fermées");
        Objects.requireNonNull(equipe);
        if (equipes.stream().anyMatch(e -> e.nom().equals(equipe.nom()))) throw new IllegalArgumentException("Equipe déjà inscrite ou nom dupliqué");
        equipes.add(equipe);
    }
    synchronized void demarrer(List<Arbitre> arbitres) {
        if (etat != EtatTournoi.PLANIFIE) throw new IllegalStateException("Tournoi déjà lancé");
        if (equipes.size() < 4) throw new IllegalStateException("Au moins quatre équipes");
        if (arbitres.isEmpty()) throw new IllegalStateException("Aucun arbitre qualifié");
        List<Equipe> reservees = new ArrayList<>();
        try {
            for (Equipe e : equipes) { e.reserver(this, jeu); reservees.add(e); }
            List<Match> rencontres = new ArrayList<>();
            for (int i = 0; i < equipes.size(); i++) for (int j = i + 1; j < equipes.size(); j++)
                rencontres.add(new Match(equipes.get(i), equipes.get(j), jeu, arbitres.get(rencontres.size() % arbitres.size())));
            matchs.addAll(rencontres); equipes.forEach(e -> points.put(e, 0));
            etat = EtatTournoi.EN_COURS;
        } catch (RuntimeException e) { reservees.forEach(eq -> eq.liberer(this)); throw e; }
    }
    synchronized void terminerMatch(Match match, boolean incident, StrategieVainqueur strategie, Random random) {
        if (etat != EtatTournoi.EN_COURS || !matchs.contains(match)) throw new IllegalStateException();
        match.terminer(incident, strategie, random);
        match.vainqueur().ifPresent(e -> points.merge(e, 3, Integer::sum));
    }
    public synchronized VueTournoi consulter() {
        Map<String, Integer> classement = new LinkedHashMap<>();
        equipes.forEach(e -> classement.put(e.nom(), points.getOrDefault(e, 0)));
        return new VueTournoi(etat, classement, matchs.stream().map(m -> new VueTournoi.ResultatMatch(
            m.premiere().nom(), m.seconde().nom(), m.etat(), m.vainqueur().map(Equipe::nom).orElse(null))).toList());
    }
    public synchronized Map<Equipe, Integer> pointsTournoi() { return Map.copyOf(points); }
    public synchronized List<Equipe> classement() {
        return equipes.stream().sorted(Comparator.<Equipe>comparingInt(e -> points.getOrDefault(e, 0)).reversed()
            .thenComparing(Equipe::nom)).toList();
    }
    public synchronized Map<Equipe, List<Match>> matchsParVainqueur() {
        Map<Equipe, List<Match>> groupes = new LinkedHashMap<>();
        for (Equipe e : classement()) {
            List<Match> gagnes = matchs.stream().filter(m -> m.vainqueur().orElse(null) == e).toList();
            if (!gagnes.isEmpty()) groupes.put(e, gagnes);
        }
        return Collections.unmodifiableMap(groupes);
    }
    synchronized Rapport cloturer() {
        int joues = (int) matchs.stream().filter(m -> m.etat() == EtatMatch.JOUE).count();
        String gagnant = joues == 0 ? null : classement().getFirst().nom();
        etat = EtatTournoi.TERMINE;
        equipes.forEach(e -> e.liberer(this));
        return new Rapport(nom, date, jeu.nom(), matchs.size(), joues, gagnant);
    }
    void publier(Rapport resultat) { rapport.complete(resultat); }
    void echec(Throwable cause) { rapport.completeExceptionally(cause); }
    public Rapport attendreRapport(Duration delai) throws InterruptedException, ExecutionException, TimeoutException {
        if (delai.isNegative()) throw new IllegalArgumentException("Délai négatif");
        return rapport.get(delai.toNanos(), TimeUnit.NANOSECONDS);
    }
    @Override public int compareTo(Tournoi autre) {
        int ordre = date.compareTo(autre.date); return ordre != 0 ? ordre : nom.compareTo(autre.nom);
    }
}
