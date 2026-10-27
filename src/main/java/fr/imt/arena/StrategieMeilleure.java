package fr.imt.arena;

import java.util.Random;

public final class StrategieMeilleure implements StrategieVainqueur {
    @Override
    public Equipe choisir(Equipe premiere, Equipe seconde, Random random) {
        int ordre = Classe.CLASSEMENT.compare(premiere, seconde);
        if (ordre <= 0) {
            return premiere;
        }
        return seconde;
    }
}
