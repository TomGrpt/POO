package fr.imt.arena.strategies;

import fr.imt.arena.metier.Equipe;

import java.util.Random;

// Tirage au hasard
public final class TirageEquitable implements StrategieVictoire {
    @Override
    public Equipe choisir(Equipe premiere, Equipe seconde, Random hasard) {
        if (hasard.nextBoolean()) {
            return premiere;
        }
        return seconde;
    }
}
