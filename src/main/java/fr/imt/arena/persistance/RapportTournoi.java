package fr.imt.arena.persistance;

import fr.imt.arena.metier.Tournoi;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
// Résumé persistant
public class RapportTournoi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomTournoi;
    private LocalDate date;
    private String jeu;
    private int matchsPrevus;
    private int matchsJoues;
    private String vainqueur;
    private boolean reussite;

    protected RapportTournoi() {}

    public RapportTournoi(Tournoi tournoi, int joues, String vainqueur) {
        if (joues < 0 || joues > tournoi.getNombreMatchs()) {
            throw new IllegalArgumentException("Nombre de matchs incohérent");
        }
        this.nomTournoi = tournoi.getNom();
        this.date = tournoi.getDate();
        this.jeu = tournoi.getJeu().getNom();
        this.matchsPrevus = tournoi.getNombreMatchs();
        this.matchsJoues = joues;
        this.vainqueur = vainqueur;
        this.reussite = 10L * joues >= 7L * matchsPrevus;
    }

    public Long getId() {
        return id;
    }

    public String getNomTournoi() {
        return nomTournoi;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getJeu() {
        return jeu;
    }

    public int getMatchsPrevus() {
        return matchsPrevus;
    }

    public int getMatchsJoues() {
        return matchsJoues;
    }

    public String getVainqueur() {
        return vainqueur;
    }

    public boolean isReussite() {
        return reussite;
    }

    @Override
    public String toString() {
        return nomTournoi
                + " : "
                + matchsJoues
                + "/"
                + matchsPrevus
                + " matchs, vainqueur="
                + vainqueur
                + ", réussite="
                + reussite;
    }
}
