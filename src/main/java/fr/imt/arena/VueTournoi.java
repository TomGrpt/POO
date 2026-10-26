package fr.imt.arena;

import java.util.List;
import java.util.Map;

/** Photographie cohérente, indépendante des mutations ultérieures de la simulation. */
public record VueTournoi(EtatTournoi etat, Map<String, Integer> points, List<ResultatMatch> matchs) {
    public VueTournoi { points = Map.copyOf(points); matchs = List.copyOf(matchs); }
    public record ResultatMatch(String premiere, String seconde, EtatMatch etat, String vainqueur) { }
}
