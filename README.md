# IMT Arena — trois modes d’exécution

Ce projet reprend le code fourni dans `IMT_Arena_Simple.zip`. Il simule des tournois e-sport, calcule les résultats et conserve les rapports dans une base H2.

Le code utilise des classes ordinaires, des boucles et des conditions avec accolades. Les commentaires Java contiennent **1 à 5 mots**.

[Télécharger le projet avec les trois Main](https://github.com/TomGrpt/POO/raw/refs/heads/main/livraison/IMT_Arena_3_Main.zip)

## Lancer le projet

Installer un JDK 17 ou supérieur et Maven 3.9, puis ouvrir un terminal à la racine :

```sh
mvn clean verify
mvn exec:java
```

Le premier lancement télécharge les dépendances. Le JAR ne contient pas toutes ces dépendances : utiliser `mvn exec:java` pour la démonstration. Dans IntelliJ ou Eclipse, importer `pom.xml`, puis lancer `fr.imt.arena.application.Main`.

Dans le cloud préparé, remplacer `mvn` par `./mvn-cloud`. Ce lanceur utilise les outils installés dans `/workspace/tools` ; il est spécifique au cloud. Sur un ordinateur personnel, les commandes Maven standard suffisent.

## Trois Main séparés

Chaque fichier possède son propre `public static void main` et prépare ses participants indépendants :

- [MainSequentiel.java](src/main/java/fr/imt/arena/application/MainSequentiel.java) : un tournoi après l’autre.
- [MainMultithread.java](src/main/java/fr/imt/arena/application/MainMultithread.java) : un thread dédié par tournoi.
- [MainPool.java](src/main/java/fr/imt/arena/application/MainPool.java) : un pool de trois threads.

Lancer le fichier voulu dans l’IDE ou utiliser une des commandes suivantes :

```sh
mvn compile exec:java -Dexec.mainClass=fr.imt.arena.application.MainSequentiel
mvn compile exec:java -Dexec.mainClass=fr.imt.arena.application.MainMultithread
mvn compile exec:java -Dexec.mainClass=fr.imt.arena.application.MainPool
```

Le gestionnaire et `ModeExecution` sont inclus. Le fichier `Main.java` conserve aussi le comparateur des trois modes.

## Choisir le mode du Main

Sans argument, Main compare les trois modes successivement. Chaque mode démarre avec trois tournois et des données neuves. Chaque tournoi possède quatre équipes et un arbitre propres ; les ressources ne sont pas partagées entre tournois.

```sh
mvn compile exec:java -Dexec.args="tous"
mvn compile exec:java -Dexec.args="sequentiel"
mvn compile exec:java -Dexec.args="threads"
mvn compile exec:java -Dexec.args="pool"
```

| Mode | Exécution réelle | Attente |
|---|---|---|
| `sequentiel` | `FutureTask.run()` dans le thread appelant, sans worker de simulation. | Chaque tournoi finit avant de lancer le suivant. |
| `threads` | `new Thread(travail, nom).start()` crée un thread par tournoi. | Les trois lancements précèdent l'attente des rapports. |
| `pool` | `Executors.newFixedThreadPool(3)` reçoit les tâches avec `execute`. | Les trois lancements précèdent l'attente des rapports. |

Le code des trois exemples est dans `Main.lancerSequentiel`, `Main.lancerAvecThreads` et `Main.lancerAvecPool`. Le choix réel d'exécution est dans `GestionnaireTournois.lancerSuivant`. Le même calcul de tournoi est utilisé dans les trois cas : ajouter des threads autour d'un lancement qui utiliserait toujours le pool ne permettrait pas cette comparaison.

[ModeExecution](src/main/java/fr/imt/arena/metier/ModeExecution.java) est une petite énumération : `SEQUENTIEL`, `THREADS`, `POOL`. Les appels utilisent ce paramètre :

```java
gestion.lancerSuivant(42, new TirageEquitable(), ModeExecution.SEQUENTIEL);
gestion.lancerSuivant(42, new TirageEquitable(), ModeExecution.THREADS);
gestion.lancerSuivant(42, new TirageEquitable(), ModeExecution.POOL);
```

Ces lignes sont trois alternatives, pas trois appels à recopier sur un gestionnaire contenant un seul tournoi. L'ancienne méthode à deux paramètres conserve le mode pool par défaut.

Le Main affiche une durée observée par mode, sauvegarde comprise. Avec six matchs de 100 ms et une arène par tournoi, la cible de simulation est 600 ms par tournoi : environ 1 800 ms en séquentiel et 600 ms en parallèle, auxquels s'ajoutent les coûts CPU/JPA. La sauvegarde reste sous le verrou commun et se fait une à la fois ; les durées mesurées ne sont pas une promesse de gain fixe.

Les mêmes graines (42, 43, 44) et les mêmes équipes neuves permettent de comparer les résultats. Le mode `tous` ajoute neuf rapports dans la base ; un mode seul en ajoute trois. Les threads dédiés sont joints, le pool est fermé et toutes les tâches sont attendues avant de fermer le dépôt.

## Nouvelle arborescence

```text
src/
├── main/
│   ├── java/fr/imt/arena/
│   │   ├── application/
│   │   │   ├── Main.java
│   │   │   ├── MainSequentiel.java
│   │   │   ├── MainMultithread.java
│   │   │   └── MainPool.java
│   │   ├── metier/
│   │   │   ├── Personne.java
│   │   │   ├── Joueur.java
│   │   │   ├── Coach.java
│   │   │   ├── Arbitre.java
│   │   │   ├── Equipe.java
│   │   │   ├── Match.java
│   │   │   ├── Tournoi.java
│   │   │   ├── GestionnaireTournois.java
│   │   │   ├── ModeExecution.java
│   │   │   ├── Classable.java
│   │   │   ├── EtatTournoi.java
│   │   │   ├── Genre.java
│   │   │   ├── StatutJoueur.java
│   │   │   └── Verrou.java
│   │   ├── jeux/
│   │   │   ├── Jeu.java
│   │   │   ├── JeuSolo.java
│   │   │   └── JeuEquipe.java
│   │   ├── strategies/
│   │   │   ├── StrategieVictoire.java
│   │   │   ├── TirageEquitable.java
│   │   │   └── MeilleureEquipe.java
│   │   ├── persistance/
│   │   │   ├── RapportTournoi.java
│   │   │   └── DepotRapports.java
│   │   └── util/
│   │       └── Textes.java
│   └── resources/META-INF/
│       └── persistence.xml
└── test/java/fr/imt/arena/metier/
    └── ArenaTest.java
```

Chaque dossier Java correspond à un **package**. Par exemple, `jeux/Jeu.java` commence par `package fr.imt.arena.jeux;`. Un `import` permet d'utiliser une classe située dans un autre package.

Les personnes, équipes, matchs, tournois et leur gestionnaire partagent le package `metier`. Leurs méthodes internes, comme `gagner`, `affecter` et `reserver`, n'ont donc pas besoin de devenir publiques. Les séparer davantage demanderait une autre organisation des échanges : cette version conserve un groupe métier cohérent et simple.

## Où commencer la lecture ?

1. [Personne](src/main/java/fr/imt/arena/metier/Personne.java), puis [Joueur](src/main/java/fr/imt/arena/metier/Joueur.java), [Coach](src/main/java/fr/imt/arena/metier/Coach.java) et [Arbitre](src/main/java/fr/imt/arena/metier/Arbitre.java).
2. [Equipe](src/main/java/fr/imt/arena/metier/Equipe.java) : membres, transferts et points.
3. [Jeu](src/main/java/fr/imt/arena/jeux/Jeu.java), [JeuSolo](src/main/java/fr/imt/arena/jeux/JeuSolo.java) et [JeuEquipe](src/main/java/fr/imt/arena/jeux/JeuEquipe.java).
4. [Match](src/main/java/fr/imt/arena/metier/Match.java) et [Tournoi](src/main/java/fr/imt/arena/metier/Tournoi.java).
5. [GestionnaireTournois](src/main/java/fr/imt/arena/metier/GestionnaireTournois.java) : registres, vérifications et simulation.
6. [RapportTournoi](src/main/java/fr/imt/arena/persistance/RapportTournoi.java) et [DepotRapports](src/main/java/fr/imt/arena/persistance/DepotRapports.java).
7. [Main](src/main/java/fr/imt/arena/application/Main.java), puis [ArenaTest](src/test/java/fr/imt/arena/metier/ArenaTest.java).

[EXPLICATION.md](EXPLICATION.md) présente les étapes du programme. Le [dossier de conception](Dossier_conception.pdf) contient deux pages ; le [diagramme Mermaid](docs/classes.mmd) est modifiable.

## Les classes en quelques mots

| Dossier | Rôle |
|---|---|
| `application` | Créer les exemples et afficher les résultats. |
| `metier` | Représenter les participants et appliquer les règles du tournoi. |
| `jeux` | Définir combien de joueurs sélectionner. |
| `strategies` | Choisir quelle équipe gagne. |
| `persistance` | Enregistrer et rechercher les rapports. |
| `util` | Vérifier les textes obligatoires. |

Personne contient les données communes. Joueur, Coach et Arbitre en héritent. Equipe réalise les transferts en mettant à jour les deux côtés de l'appartenance. Tournoi contient les inscriptions, l'état, les matchs publiés et le résultat attendu. Le gestionnaire prépare et lance la simulation.

[Classable](src/main/java/fr/imt/arena/metier/Classable.java) définit la lecture des points. [Genre](src/main/java/fr/imt/arena/metier/Genre.java), [StatutJoueur](src/main/java/fr/imt/arena/metier/StatutJoueur.java) et [EtatTournoi](src/main/java/fr/imt/arena/metier/EtatTournoi.java) sont des énumérations. [Textes](src/main/java/fr/imt/arena/util/Textes.java) refuse les textes vides et retire les espaces externes.

## Le parcours d'un tournoi

1. Main enregistre les personnes, les équipes et les jeux.
2. Il inscrit les équipes au tournoi, puis le planifie.
3. `lancerSuivant` prend le tournoi le plus proche par date, puis par nom.
4. Le gestionnaire vérifie les équipes et réserve un arbitre disponible et qualifié.
5. La tâche est exécutée directement, dans un thread dédié ou dans le pool selon le mode. Le Main peut continuer dans les deux modes parallèles.
6. Deux boucles créent chaque paire d'équipes une seule fois.
7. Les résultats sont tirés au rythme prévu ; un incident annule la rencontre.
8. À la fin, le gestionnaire applique les points et les statistiques, crée le rapport et le sauvegarde.
9. Il publie l'état terminé et libère les réservations.
10. `attendreRapport` récupère le résultat, avec une durée maximale d'attente.

Pour quatre équipes, les rencontres sont AB, AC, AD, BC, BD et CD. Pour `n` équipes, il y a `n × (n − 1) / 2` matchs.

## Réglages simples

```java
gestion.lancerSuivant(42, new TirageEquitable());
tournoi.attendreRapport(10, TimeUnit.SECONDS);
```

`42` est la graine du hasard : elle permet de reproduire les tirages avec les mêmes données. Les extraits ci-dessus sont des appels à intégrer dans un programme ; Main fournit l'exemple complet.

- `TEMPS_PAR_MATCH_MS` vaut 100 ms par défaut.
- Le constructeur du gestionnaire permet de changer la durée et le risque d'incident.
- [TirageEquitable](src/main/java/fr/imt/arena/strategies/TirageEquitable.java) donne une chance sur deux à chaque équipe.
- [MeilleureEquipe](src/main/java/fr/imt/arena/strategies/MeilleureEquipe.java) choisit selon les points existants, puis le nom.
- Les deux classes implémentent [StrategieVictoire](src/main/java/fr/imt/arena/strategies/StrategieVictoire.java). C'est le pattern Strategy.

## Collections et concurrence

Les `ArrayList` conservent des listes, les `HashMap` retrouvent les objets par identifiant ou nom, le `HashSet` évite les qualifications en double. La `PriorityQueue` choisit le prochain tournoi. Les classements sont des copies de listes triées avec un comparateur explicite. La `LinkedHashMap` regroupe les matchs par gagnant en gardant leur ordre de première apparition.

[Verrou](src/main/java/fr/imt/arena/metier/Verrou.java) contient le verrou commun. Les blocs `synchronized` protègent ensemble les transferts, les réservations et la publication finale. Le pool contient trois threads ; les threads dédiés ne passent pas par ce pool. Le compteur `enCours` empêche un quatrième tournoi actif, tous modes confondus.

Les pauses, le choix du vainqueur et l'attente du rapport se font hors verrou. La sauvegarde finale reste sous le verrou commun, comme dans le code fourni : cela facilite une publication cohérente, mais peut retarder temporairement les consultations et les autres clôtures. Ce choix convient à cette petite simulation.

## Base de données

H2 stocke les rapports dans `data/arena.mv.db`, relativement au dossier de lancement. JPA définit l'enregistrement des objets ; Hibernate le réalise. Seul RapportTournoi est une entité persistée. Le fichier [persistence.xml](src/main/resources/META-INF/persistence.xml) référence désormais son nouveau package.

`DepotRapports` crée un EntityManager par opération, effectue une transaction pour l'enregistrement et ferme l'EntityManager automatiquement. `tous()` lit tous les rapports ; `parJeu(nom)` filtre sur un jeu.

Relancer Main ajoute des rapports. Ne pas lancer plusieurs JVM simultanément sur ce fichier. Pour effacer les rapports, fermer le programme puis supprimer `data/` ; cela supprime définitivement les données.

## Règles conservées du ZIP

- Ajouter un joueur déjà affecté le transfère si les deux équipes sont libres. Son compteur de victoires depuis arrivée est remis à zéro ; son statut professionnel reste acquis.
- Le coach transféré garde ses victoires de carrière. L'ancienne équipe perd son coach.
- Aucun effectif ne change pendant un tournoi en cours.
- Les premiers joueurs de la liste composent l'équipe alignée ; les autres restent remplaçants.
- Une victoire donne trois points et fait progresser tous les joueurs de l'effectif.
- Les points généraux se cumulent. Le vainqueur d'un tournoi utilise ses points locaux, puis le nom. Sans match joué, son nom est « Aucun ».
- Un arbitre disponible est réservé pour tout le tournoi. Les arènes sont un facteur de débit, sans planning détaillé par personne.
- La cible temporelle est `matchs × temps / arènes`. Les résultats sont espacés de `temps / arènes` ; il n'y a pas de thread par match. Le système et le stockage ajoutent un surcoût.
- Les matchs et les statistiques sont publiés ensemble à la fin, pas progressivement.
- Un incident ne donne ni points, ni victoire, ni match arbitré comptabilisé.
- Le tournoi réussit si `10 × joués >= 7 × prévus`.
- Si le prochain tournoi est invalide ou occupé, le lancement échoue sans passer silencieusement au suivant.
- Utiliser un seul gestionnaire pour l'application. Les stratégies personnalisées doivent terminer et respecter l'interruption.
- `close()` attend les tâches, joint les threads dédiés et attend la fermeture du pool. Une interruption de l’appelant demande l’arrêt des threads possédés par le gestionnaire. Une stratégie qui ignore indéfiniment l’interruption peut bloquer cette fermeture. Une simulation séquentielle reste exécutée par son appelant.
- Une erreur de stratégie ou de sauvegarde est transmise par `ExecutionException`. Les réservations sont libérées ; le rapport reste consultable en mémoire.

## Vérifications et livraison

Les **17 tests existants** ont été conservés. Sept exécutions supplémentaires vérifient les modes : **24 tests passent** au total. Ils couvrent les règles initiales, le thread réellement utilisé, l’accès aux consultations hors verrou, trois tournois effectivement parallèles et la libération après une fermeture interrompue. Voir [VERIFICATION.txt](VERIFICATION.txt) pour les résultats réellement obtenus.

Le ZIP contient les sources, les tests, les ressources Maven/JPA et les documents à jour. Il exclut les dépendances, `.git`, les résultats de compilation et les bases remplies.

Les scripts dans `scripts/` servent à la préparation cloud ou à la régénération du dossier. Python, ReportLab, Graphviz et Open Sans sont nécessaires uniquement pour régénérer le PDF fourni ; ils ne servent pas à compiler l'application Java.
