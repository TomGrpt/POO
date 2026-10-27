package fr.imt.arena;

/** Raccourcis de création utilisés aussi par les tests. */
public final class Strategies {
    private Strategies() {
    }

    public static StrategieVainqueur equitable() {
        return new StrategieEquitable();
    }

    public static StrategieVainqueur meilleure() {
        return new StrategieMeilleure();
    }

    public static StrategieVainqueur ponderee() {
        return new StrategiePonderee();
    }
}
