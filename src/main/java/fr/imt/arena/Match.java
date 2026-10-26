package fr.imt.arena;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

public final class Match {
    private final Equipe premiere, seconde;
    private final Jeu jeu;
    private final Arbitre arbitre;
    private final List<Joueur> compositionPremiere, compositionSeconde;
    private EtatMatch etat = EtatMatch.PREVU;
    private Equipe vainqueur;
    public Match(Equipe premiere, Equipe seconde, Jeu jeu, Arbitre arbitre) {
        this.premiere = Objects.requireNonNull(premiere); this.seconde = Objects.requireNonNull(seconde);
        this.jeu = Objects.requireNonNull(jeu); this.arbitre = Objects.requireNonNull(arbitre);
        if (premiere == seconde) throw new IllegalArgumentException("Match contre soi-même");
        if (!arbitre.estQualifie(jeu)) throw new IllegalArgumentException("Arbitre non qualifié");
        compositionPremiere = jeu.selectionner(premiere.joueurs());
        compositionSeconde = jeu.selectionner(seconde.joueurs());
    }
    public Equipe premiere() { return premiere; }
    public Equipe seconde() { return seconde; }
    public Jeu jeu() { return jeu; }
    public Arbitre arbitre() { return arbitre; }
    public List<Joueur> compositionPremiere() { return compositionPremiere; }
    public List<Joueur> compositionSeconde() { return compositionSeconde; }
    public synchronized EtatMatch etat() { return etat; }
    public synchronized Optional<Equipe> vainqueur() { return Optional.ofNullable(vainqueur); }
    synchronized void terminer(boolean incident, StrategieVainqueur strategie, Random random) {
        if (etat != EtatMatch.PREVU) throw new IllegalStateException("Match déjà clos");
        if (incident) { etat = EtatMatch.ANNULE; return; }
        Equipe choix = strategie.choisir(premiere, seconde, random);
        if (choix != premiere && choix != seconde) throw new IllegalStateException("Vainqueur étranger");
        choix.victoire(); arbitre.matchJoue(); vainqueur = choix; etat = EtatMatch.JOUE;
    }
    public boolean concerne(Equipe equipe) { return premiere == equipe || seconde == equipe; }
}
