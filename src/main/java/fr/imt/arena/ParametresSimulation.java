package fr.imt.arena;

import java.time.Duration;
import java.util.Objects;

public record ParametresSimulation(Duration tempsParMatch, double probabiliteIncident, StrategieVainqueur strategie) {
    public ParametresSimulation {
        Objects.requireNonNull(tempsParMatch); Objects.requireNonNull(strategie);
        if (tempsParMatch.isNegative() || !Double.isFinite(probabiliteIncident)
                || probabiliteIncident < 0 || probabiliteIncident > 1) throw new IllegalArgumentException("Simulation invalide");
        tempsParMatch.toNanos(); // Détecter immédiatement les durées non représentables.
    }
    public long dureeNanos(int matchs, int arenes) {
        return Math.multiplyExact(tempsParMatch.toNanos(), matchs) / arenes;
    }
}
