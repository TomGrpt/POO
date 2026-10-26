package fr.imt.arena;

import java.util.Random;

@FunctionalInterface
public interface StrategieVainqueur {
    Equipe choisir(Equipe premiere, Equipe seconde, Random random);
}
