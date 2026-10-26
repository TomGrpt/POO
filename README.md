# IMT Arena — Tournois e-sport

Bibliothèque Java 21 et démonstration console pour planifier, simuler et archiver des tournois toutes rondes. Sans Spring : Maven, JUnit 5, Hibernate/JPA et H2 fichier.

## Accéder au projet

- [Code Java](src/main/java/fr/imt/arena/) — [programme Main](src/main/java/fr/imt/arena/Main.java)
- [Tests JUnit](src/test/java/fr/imt/arena/)
- [Dossier de conception PDF — 2 pages](docs/DOSSIER_CONCEPTION.pdf)
- [Analyse et traçabilité](docs/ANALYSE.md)
- [Préparation à l’entretien](docs/ENTRETIEN.md)
- [Télécharger le ZIP complet](https://github.com/TomGrpt/POO/raw/refs/heads/main/livraison/TP_Tournoi_Esport.zip)

## Compiler, tester, exécuter

Prérequis : **JDK 21 complet** (pas seulement un JRE), Maven 3.9+, accès Maven Central au premier lancement.
Depuis la racine du projet :

```sh
mvn clean verify
mvn exec:java
```

La démonstration crée six équipes de trois joueurs, deux arbitres, un jeu solo et un jeu collectif. Deux tournois successifs illustrent le partage des équipes, les classements, les matchs, les notifications et les rapports. Chaque tournoi a sa propre graine fixe ; les rapports s'ajoutent à chaque exécution.

Dans l'environnement cloud préparé, utiliser `./mvn-cloud` à la place de `mvn`. Il sélectionne le JDK complet, le routage proxy et le cache Maven inscriptible installés hors du dépôt. Ce raccourci est facultatif et spécifique à `/workspace` ; sur votre machine, les commandes Maven standard suffisent.

## Persistance

Seuls les rapports sont des entités JPA. Base par défaut : `jdbc:h2:file:./data/arena`, donc fichier `data/arena.mv.db` relatif au répertoire d'exécution. Le dossier est créé automatiquement. Aucun serveur H2 séparé, aucune clé ni variable secrète nécessaire. Fermer Arena avant DepotJpa (ordre assuré par try-with-resources). Ne pas lancer deux JVM simultanées sur ce même fichier. Pour repartir à zéro, fermer l'application puis supprimer `data/` ; cette opération efface les rapports.

`DepotJpa.tous()` et `parJeu(String)` renvoient les rapports détachés. Une erreur de stockage est propagée par `attendreRapport`, jamais présentée comme une sauvegarde réussie.

## Organisation

- `src/main/java/fr/imt/arena/` : personnes, jeux, équipes, matchs, tournoi, simulateur, service Arena, stratégies, observateurs, dépôt et Main.
- `src/main/resources/META-INF/persistence.xml` : configuration JPA/H2.
- `src/test/java/fr/imt/arena/` : règles métier, tournois, simulation, concurrence, persistance fichier.
- `docs/DOSSIER_CONCEPTION.pdf` : dossier de **deux pages**, diagramme compris.
- `docs/classes.mmd` : source Mermaid complète du diagramme de classes.
- `docs/ANALYSE.md` : analyse préalable et matrice exigences / implantation / tests.
- `docs/ENTRETIEN.md` : dix questions et réponses adaptées au code.
- `docs/VALIDATION.md` : vérifications effectivement exécutées et limites.

## Conventions métier et temporelles

Les identifiants des personnes sont uniques, comme supposé dans le PDF. Le registre Arena rejette les doublons d'identité/noms ; utiliser une seule Arena pour gérer une saison. Un nom de jeu, sensible à la casse après suppression des espaces externes, identifie sa définition. Les équipes doivent être enregistrées pour apparaître au classement général ; inscrire une équipe au tournoi ne l'ajoute pas automatiquement au registre.

Les points généraux et victoires des coachs sont cumulatifs ; le vainqueur d'un tournoi dépend de ses points locaux, puis du nom en cas d'égalité. S'il n'y a aucun match joué, il n'y a pas de vainqueur. Tous les joueurs de l'équipe progressent après une victoire, même ceux non sélectionnés ; une promotion acquise n'est pas perdue lors d'un transfert. Les effectifs et le coach ne changent pas pendant la réservation.

Une tâche de tournoi représente des arènes logiques : résultats par lots d'au plus A matchs. Durée cible = M × tempsParMatch / A ; le dernier lot est proratisé si nécessaire. Exemple du PDF : 15 × 100 ms / 3 = 500 ms, auxquels s'ajoutent les coûts d'exécution et de persistance. Il n'est pas ajouté de contrainte de planning interdisant une équipe ou un arbitre dans plusieurs rencontres logiques simultanées. Les arbitres occupés sont ceux engagés dans au moins un tournoi ; un arbitre peut être partagé entre tournois, seule sa qualification étant imposée. Les matchs annulés n'augmentent ni les points ni le nombre de matchs arbitrés.

`Tournoi.consulter()` fournit une photographie immuable cohérente (état, points locaux, résultats). Les autres accesseurs sont protégés individuellement ; plusieurs appels successifs peuvent naturellement observer l'avancement. `attendreRapport(Duration)` borne l'attente et inclut la fin de la sauvegarde et la libération des ressources. Les collections rendues sont non modifiables.

Les stratégies et observateurs sont des extensions synchrones : elles doivent terminer rapidement, respecter l'interruption et ne pas appeler `Arena.close()` depuis un worker. Leurs exceptions sont gérées ; un code utilisateur qui ignore indéfiniment l'interruption ne peut pas être arrêté de force par Java. Pour un jeu de tests déterministe, employer des stratégies sans état mutable partagé et un dépôt temporaire.

## Livrable et dossier

L'archive livrée contient sources, configurations, tests, README et documentation ; elle exclut `.git`, `target`, les caches et les bases de démonstration. Le PDF de conception est déjà généré : aucun outil de génération documentaire n'est requis pour compiler le projet. La source Mermaid peut être visualisée dans un éditeur compatible.
