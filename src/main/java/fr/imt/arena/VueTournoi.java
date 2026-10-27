package fr.imt.arena;

import java.util.List;
import java.util.Map;

/** Une photographie du tournoi qui ne change plus après sa création. */
public final class VueTournoi {
    private final EtatTournoi etat;
    private final Map<String, Integer> points;
    private final List<ResultatMatch> matchs;

    public VueTournoi(EtatTournoi etat, Map<String, Integer> points, List<ResultatMatch> matchs) {
        this.etat = etat;
        this.points = Map.copyOf(points);
        this.matchs = List.copyOf(matchs);
    }

    public EtatTournoi etat() {
        return etat;
    }

    public Map<String, Integer> points() {
        return points;
    }

    public List<ResultatMatch> matchs() {
        return matchs;
    }
}
