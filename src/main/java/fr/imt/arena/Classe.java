package fr.imt.arena;

import java.util.Comparator;

public interface Classe {
    String nom();
    int points();
    Comparator<Equipe> CLASSEMENT = Comparator.comparingInt(Equipe::points).reversed().thenComparing(Equipe::nom);
}
