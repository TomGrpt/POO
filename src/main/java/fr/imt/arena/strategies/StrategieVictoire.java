package fr.imt.arena.strategies;

import fr.imt.arena.metier.Equipe;

import java.util.Random;

// Contrat de victoire
public interface StrategieVictoire {
    Equipe choisir(Equipe premiere, Equipe seconde, Random hasard);
}
