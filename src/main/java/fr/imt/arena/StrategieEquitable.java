package fr.imt.arena;

import java.util.Random;

public final class StrategieEquitable implements StrategieVainqueur {
    @Override
    public Equipe choisir(Equipe premiere, Equipe seconde, Random random) {
        if (random.nextBoolean()) {
            return premiere;
        }
        return seconde;
    }
}
