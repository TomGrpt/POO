package fr.imt.arena.strategies;

import fr.imt.arena.metier.Equipe;

import java.util.Random;

// Classement prioritaire
public final class MeilleureEquipe implements StrategieVictoire {
    @Override
    public Equipe choisir(Equipe premiere, Equipe seconde, Random hasard) {
        if (premiere.getPoints() > seconde.getPoints()) {
            return premiere;
        }
        if (seconde.getPoints() > premiere.getPoints()) {
            return seconde;
        }
        if (premiere.getNom().compareTo(seconde.getNom()) <= 0) {
            return premiere;
        }
        return seconde;
    }
}
