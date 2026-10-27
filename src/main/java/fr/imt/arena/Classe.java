package fr.imt.arena;

import java.util.Comparator;

public interface Classe {
    String nom();
    int points();

    Comparator<Equipe> CLASSEMENT = new Comparator<Equipe>() {
        @Override
        public int compare(Equipe premiere, Equipe seconde) {
            // Inverser les arguments place les points les plus élevés en premier.
            int ordre = Integer.compare(seconde.points(), premiere.points());
            if (ordre == 0) {
                ordre = premiere.nom().compareTo(seconde.nom());
            }
            return ordre;
        }
    };
}
