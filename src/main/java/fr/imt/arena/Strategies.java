package fr.imt.arena;

public final class Strategies {
    private Strategies() { }
    public static StrategieVainqueur equitable() { return (a, b, r) -> r.nextBoolean() ? a : b; }
    public static StrategieVainqueur meilleure() {
        return (a, b, r) -> Classe.CLASSEMENT.compare(a, b) <= 0 ? a : b;
    }
    public static StrategieVainqueur ponderee() {
        return (a, b, r) -> r.nextDouble() < (a.points() + 1.0) / (a.points() + (double) b.points() + 2.0) ? a : b;
    }
}
