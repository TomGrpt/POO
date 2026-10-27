package fr.imt.arena;

import java.time.Duration;
import java.util.Objects;

public final class ParametresSimulation {
    private final Duration tempsParMatch;
    private final double probabiliteIncident;
    private final StrategieVainqueur strategie;

    public ParametresSimulation(Duration tempsParMatch, double probabiliteIncident,
                                StrategieVainqueur strategie) {
        Objects.requireNonNull(tempsParMatch);
        Objects.requireNonNull(strategie);
        if (tempsParMatch.isNegative()) {
            throw new IllegalArgumentException("La durée ne peut pas être négative");
        }
        if (!Double.isFinite(probabiliteIncident)
                || probabiliteIncident < 0 || probabiliteIncident > 1) {
            throw new IllegalArgumentException("La probabilité doit être entre 0 et 1");
        }
        tempsParMatch.toNanos(); // Refuser une durée trop grande dès la création.
        this.tempsParMatch = tempsParMatch;
        this.probabiliteIncident = probabiliteIncident;
        this.strategie = strategie;
    }

    public Duration tempsParMatch() {
        return tempsParMatch;
    }

    public double probabiliteIncident() {
        return probabiliteIncident;
    }

    public StrategieVainqueur strategie() {
        return strategie;
    }

    public long dureeNanos(int nombreMatchs, int nombreArenes) {
        long dureeSansArenes = Math.multiplyExact(tempsParMatch.toNanos(), nombreMatchs);
        return dureeSansArenes / nombreArenes;
    }
}
