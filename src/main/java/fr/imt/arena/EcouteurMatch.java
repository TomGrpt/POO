package fr.imt.arena;

/** Observer : les callbacks reçoivent un match terminé, hors verrou du tournoi. */
@FunctionalInterface
public interface EcouteurMatch { void matchTermine(Match match); }
