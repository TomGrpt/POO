package fr.imt.arena;

import java.util.Random;

public interface StrategieVainqueur {
    Equipe choisir(Equipe premiere, Equipe seconde, Random random);
}
