package fr.imt.arena;

import java.util.Random;

public final class StrategiePonderee implements StrategieVainqueur {
    @Override
    public Equipe choisir(Equipe premiere, Equipe seconde, Random random) {
        double poidsPremiere = premiere.points() + 1.0;
        double poidsSeconde = seconde.points() + 1.0;
        double chancePremiere = poidsPremiere / (poidsPremiere + poidsSeconde);
        if (random.nextDouble() < chancePremiere) {
            return premiere;
        }
        return seconde;
    }
}
