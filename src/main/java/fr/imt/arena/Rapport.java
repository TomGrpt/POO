package fr.imt.arena;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rapports")
public class Rapport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String nomTournoi;
    @Column(nullable = false) private LocalDateTime date;
    @Column(nullable = false) private String jeu;
    private int matchsPrevus;
    private int matchsJoues;
    private String equipeVainqueur;
    private boolean reussite;
    protected Rapport() { }
    Rapport(String nom, LocalDateTime date, String jeu, int prevus, int joues, String vainqueur) {
        this.nomTournoi = nom; this.date = date; this.jeu = jeu;
        this.matchsPrevus = prevus; this.matchsJoues = joues; this.equipeVainqueur = vainqueur;
        this.reussite = estReussi(prevus, joues);
    }
    public static boolean estReussi(int prevus, int joues) {
        if (prevus <= 0 || joues < 0 || joues > prevus) throw new IllegalArgumentException("Comptage invalide");
        return joues * 10L >= prevus * 7L;
    }
    public Long id() { return id; }
    public String nomTournoi() { return nomTournoi; }
    public LocalDateTime date() { return date; }
    public String jeu() { return jeu; }
    public int matchsPrevus() { return matchsPrevus; }
    public int matchsJoues() { return matchsJoues; }
    public String equipeVainqueur() { return equipeVainqueur; }
    public boolean reussite() { return reussite; }
    @Override public String toString() {
        return nomTournoi + " | " + jeu + " | " + matchsJoues + "/" + matchsPrevus
            + " | vainqueur=" + equipeVainqueur + " | réussi=" + reussite;
    }
}
