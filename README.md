# IMT Arena — comprendre le projet pas à pas

Ce projet Java simule des tournois e-sport. On crée des personnes et des équipes, on inscrit les équipes à des tournois, puis le programme joue les rencontres et enregistre les rapports.

Le code utilise surtout des **classes, des boucles `for` et des conditions `if`**. Chaque règle de victoire possède sa propre petite classe. Les champs sont privés et les contrôles restent dans les objets qui possèdent les données.

## Accès rapide

- [Lire le code](src/main/java/fr/imt/arena/) et [le programme Main](src/main/java/fr/imt/arena/Main.java).
- [Lire les tests](src/test/java/fr/imt/arena/).
- [Télécharger le ZIP](https://github.com/TomGrpt/POO/raw/refs/heads/main/livraison/TP_Tournoi_Esport.zip).
- [Dossier de conception, deux pages](docs/DOSSIER_CONCEPTION.pdf) et [diagramme de classes](docs/classes.mmd).
- [Exigences du sujet et tests correspondants](docs/ANALYSE.md).
- [Dix questions pour préparer l'oral](docs/ENTRETIEN.md).
- [Résultats des vérifications](docs/VALIDATION.md).

## Sommaire

1. [Lancer le projet](#1-lancer-le-projet)
2. [Par où commencer la lecture](#2-par-où-commencer-la-lecture)
3. [Les personnes](#3-les-personnes)
4. [Les jeux](#4-les-jeux)
5. [Les équipes](#5-les-équipes)
6. [Les matchs et les règles de victoire](#6-les-matchs-et-les-règles-de-victoire)
7. [Les tournois](#7-les-tournois)
8. [Arena et les collections](#8-arena-et-les-collections)
9. [La simulation et les threads](#9-la-simulation-et-les-threads)
10. [Les rapports et la base de données](#10-les-rapports-et-la-base-de-données)
11. [Ce que fait Main](#11-ce-que-fait-main)
12. [Les tests](#12-les-tests)
13. [Comment construire ce projet soi-même](#13-comment-construire-ce-projet-soi-même)
14. [Les fichiers techniques et les choix du sujet](#14-les-fichiers-techniques-et-les-choix-du-sujet)

## 1. Lancer le projet

Il faut un **JDK 21 complet** et **Maven 3.9 ou supérieur**. Le JDK permet de compiler ; Maven télécharge les dépendances, compile les fichiers et lance les tests. Le premier lancement nécessite un accès à Maven Central.

Dans un terminal, à la racine du projet :

```sh
mvn clean verify
mvn exec:java
```

La première commande reconstruit le projet et exécute les tests. La seconde lance la démonstration. `mvn test` permet de relancer seulement la compilation nécessaire et les tests.

Dans le cloud déjà préparé, remplacer `mvn` par `./mvn-cloud` :

```sh
cd /workspace/POO
./mvn-cloud clean verify
./mvn-cloud exec:java
```

Sur un ordinateur personnel, les commandes Maven ordinaires suffisent. Il n'y a pas de serveur web à ouvrir ni de base de données à installer séparément. Le JAR produit n'embarque pas toutes les dépendances : utiliser `mvn exec:java` pour cette démonstration.

Les messages Hibernate dans le terminal décrivent l'ouverture de la base. Après eux, le programme affiche les tournois, les résultats et les classements. À chaque lancement, il ajoute de nouveaux rapports dans la base existante.

## 2. Par où commencer la lecture

Lire d'abord **Personne → Joueur → Coach → Jeu → Equipe → Match**. Ces classes expliquent les règles du projet. Lire ensuite **Tournoi**, puis **Main**. Garder **Arena**, **Simulateur** et **DepotJpa** pour la fin : ils gèrent les threads et le stockage.

```text
src/main/java/fr/imt/arena/       Les classes de l'application
src/main/resources/META-INF/     La configuration de la base
src/test/java/fr/imt/arena/       Les tests
pom.xml                         La configuration Maven
docs/                           Le dossier, les diagrammes et les annexes
livraison/                      Le ZIP du projet
```

`package fr.imt.arena;` signifie que la classe appartient au groupe de classes `fr.imt.arena`. Les classes de ce groupe peuvent collaborer sans importer leurs voisines.

Quelques mots utiles avant de lire :

| Mot Java | Comment le comprendre dans ce projet |
|---|---|
| `private` | L'attribut est accessible uniquement depuis sa classe. |
| `public` | Une autre application peut appeler cette méthode. |
| Aucun mot avant une méthode | Les classes du même package peuvent l'appeler ; une application dans un autre package ne le peut pas. |
| `final` sur un attribut | La référence ou la valeur affectée à cet attribut ne sera pas remplacée. Cela ne rend pas automatiquement une liste immuable. |
| `final` sur une classe | On ne crée pas de sous-classe à partir d'elle. |
| `abstract` | Cette classe sert de base ; on instancie ses sous-classes concrètes. |
| `extends` | « Est une sorte de » : un Joueur est une Personne. |
| `implements` | La classe fournit les méthodes demandées par une interface. |
| `@Override` | Cette méthode remplit ou redéfinit un rôle déclaré ailleurs. |
| `synchronized` | Une seule exécution à la fois utilise le verrou de cet objet ; voir la partie sur les threads. |

Les méthodes `nom()`, `points()` ou `statut()` sont de simples méthodes de lecture, comme des getters nommés `getNom()`, `getPoints()` ou `getStatut()`. Les paramètres et les vues sont des classes ordinaires avec un constructeur et des attributs : il n'y a pas de `record` à apprendre pour les lire.

## 3. Les personnes

### La classe commune : Personne

[Personne.java](src/main/java/fr/imt/arena/Personne.java) contient ce que tous les participants ont en commun : `id`, `nom`, `prenom`, `pseudo`, `genre` et `telephone`.

Elle est abstraite : on ne crée pas `new Personne(...)`. On crée un Joueur, un Coach ou un Arbitre. Le constructeur de chaque sous-classe appelle celui de Personne avec `super(...)` pour initialiser ces informations communes.

[Genre.java](src/main/java/fr/imt/arena/Genre.java) est une énumération : une liste de valeurs autorisées. [Verifications.java](src/main/java/fr/imt/arena/Verifications.java) refuse les textes vides et enlève les espaces aux extrémités. Ce contrôle partagé évite de le recopier dans plusieurs constructeurs.

`equals` compare les identifiants des personnes. `hashCode` utilise le même identifiant pour rester cohérent avec les collections. Le sujet suppose que ces identifiants sont uniques ; le registre d'Arena refuse d'enregistrer deux fois le même identifiant.

### Joueur : une équipe et une progression

[Joueur.java](src/main/java/fr/imt/arena/Joueur.java) ajoute une équipe éventuelle, un statut et un compteur de victoires depuis son arrivée. [StatutJoueur.java](src/main/java/fr/imt/arena/StatutJoueur.java) définit `AMATEUR` et `PROFESSIONNEL`.

- `rejoindre` refuse le joueur s'il a déjà une équipe.
- `quitter` vérifie qu'il quitte la bonne équipe et remet le compteur d'arrivée à zéro.
- `victoire` augmente ce compteur. Dès trois victoires, le joueur devient professionnel.

Ces méthodes de modification sont internes au package. Depuis une autre application, on utilise `Equipe.ajouter` et `Equipe.retirer` : cela garde cohérents le rattachement du joueur et la liste de l'équipe.

**Exemple :** Alex gagne deux matchs avec A, puis rejoint B. Il doit gagner trois matchs avec B pour devenir professionnel. S'il était déjà professionnel, un transfert ne lui enlève pas ce statut.

### Coach : des victoires conservées

[Coach.java](src/main/java/fr/imt/arena/Coach.java) ressemble au joueur pour l'appartenance à une équipe. En revanche, son nombre de victoires décrit toute sa carrière : quitter une équipe ne remet pas ce compteur à zéro.

### Arbitre : des qualifications

[Arbitre.java](src/main/java/fr/imt/arena/Arbitre.java) conserve les jeux pour lesquels il est qualifié et son nombre de matchs arbitrés. Il doit avoir au moins une qualification.

L'interface [Qualifie.java](src/main/java/fr/imt/arena/Qualifie.java) impose la méthode `estQualifie(Jeu)`. Une interface décrit ici une capacité, sans imposer d'hériter d'une classe particulière.

`engager` augmente son nombre de tournois en cours ; `liberer` le diminue. `disponible()` renvoie vrai quand ce nombre est nul. C'est un compteur, car le sujet n'impose pas l'exclusivité d'un arbitre entre deux tournois. `matchJoue` compte uniquement les rencontres effectivement jouées, pas celles annulées.

## 4. Les jeux

[Jeu.java](src/main/java/fr/imt/arena/Jeu.java) est la base commune : un nom, un nombre de manches positif et une méthode `joueursParEquipe()`.

| Classe | Nombre de joueurs alignés |
|---|---|
| [JeuSolo](src/main/java/fr/imt/arena/JeuSolo.java) | Toujours un joueur par équipe. |
| [JeuCollectif](src/main/java/fr/imt/arena/JeuCollectif.java) | Un nombre défini au constructeur, de trois à cinq. |

La méthode `selectionner` vérifie la taille de l'effectif, parcourt les premiers joueurs avec une boucle et renvoie une copie non modifiable de la sélection.

```java
Jeu jeu = new JeuCollectif("Arena 3", 5, 3);
int nombre = jeu.joueursParEquipe(); // nombre vaut 3
```

La variable est de type Jeu, mais Java utilise la méthode du véritable objet JeuCollectif. C'est le **polymorphisme**. Match peut donc demander une sélection sans tester successivement tous les types de jeux.

Un nouveau jeu peut hériter de Jeu et définir sa taille ou sa façon de sélectionner les joueurs. Le test `selectionSoloEtExtensionPolymorphe` montre un jeu en duo ajouté de cette manière. Le nombre de manches est mémorisé, mais les manches ne sont pas simulées une par une.

## 5. Les équipes

[Equipe.java](src/main/java/fr/imt/arena/Equipe.java) possède un nom, des joueurs, un coach éventuel, des points et une réservation éventuelle.

| Méthode | Ce qu'elle fait |
|---|---|
| `ajouter(joueur)` | Vérifie que l'équipe est modifiable, rattache le joueur et l'ajoute à la liste. |
| `retirer(joueur)` | Vérifie sa présence, enlève son rattachement puis le retire de la liste. |
| `nommer(coach)` | Vérifie le nouveau coach avant de remplacer l'ancien. `null` permet de retirer le coach. |
| `verifier(jeu)` | Exige un coach et assez de joueurs pour ce jeu. |
| `reserver(tournoi, jeu)` | Vérifie l'équipe et la bloque pour ce tournoi. |
| `liberer(tournoi)` | Débloque l'équipe, uniquement pour le tournoi qui l'a réservée. |
| `victoire()` | Ajoute trois points, une victoire au coach et une victoire à chaque joueur. |

La réservation empêche de modifier les joueurs ou le coach pendant un tournoi. Puisqu'un joueur ou un coach ne peut appartenir qu'à une équipe, il ne peut pas être déplacé pour participer à un autre tournoi simultané.

La liste interne est privée. `joueurs()` renvoie `List.copyOf(joueurs)` : l'appelant peut la lire, mais pas y ajouter un joueur sans passer par les contrôles. Les objets Joueur contenus gardent toutefois leurs propres statistiques : une liste non modifiable ne fige pas tous ses objets.

L'interface [Classe.java](src/main/java/fr/imt/arena/Classe.java) demande un nom et des points. Son comparateur place les points élevés en premier, puis utilise le nom pour départager. « Classe » signifie ici « qui peut être classé ».

## 6. Les matchs et les règles de victoire

### Match

[Match.java](src/main/java/fr/imt/arena/Match.java) garde les équipes, le jeu, l'arbitre, les compositions et le résultat. Le constructeur refuse une équipe contre elle-même, un arbitre non qualifié ou un effectif insuffisant.

[EtatMatch.java](src/main/java/fr/imt/arena/EtatMatch.java) définit les transitions possibles :

```text
PREVU → JOUE       une équipe gagne
PREVU → ANNULE     un incident technique empêche la rencontre
```

`terminer` refuse de résoudre un match déjà clos. Avec un incident, il annule et s'arrête. Sinon, il demande une équipe gagnante à la stratégie, vérifie qu'elle participe au match, applique sa victoire et augmente le compteur de l'arbitre. Il n'y a pas de match nul.

`vainqueur()` renvoie un `Optional<Equipe>` : un petit objet qui peut contenir une équipe ou être vide. Dans Tournoi, on teste `isPresent()` avant d'utiliser `get()`. Cela exprime qu'un match prévu ou annulé n'a pas de vainqueur.

### Trois petites classes de stratégie

[StrategieVainqueur.java](src/main/java/fr/imt/arena/StrategieVainqueur.java) déclare une seule méthode : `choisir(premiere, seconde, random)`.

| Classe | Règle |
|---|---|
| [StrategieEquitable](src/main/java/fr/imt/arena/StrategieEquitable.java) | Un booléen aléatoire donne une chance sur deux à chaque équipe. |
| [StrategieMeilleure](src/main/java/fr/imt/arena/StrategieMeilleure.java) | Le classement général choisit l'équipe ; le nom départage une égalité. |
| [StrategiePonderee](src/main/java/fr/imt/arena/StrategiePonderee.java) | La chance de A vaut `(pointsA + 1) / (pointsA + pointsB + 2)`. |

La stratégie équitable se lit comme une condition ordinaire :

```java
if (random.nextBoolean()) {
    return premiere;
}
return seconde;
```

Ces classes réalisent le pattern **Strategy** : on change la règle de choix en changeant l'objet fourni, sans modifier Match. Main utilise `new StrategieEquitable()`. [Strategies.java](src/main/java/fr/imt/arena/Strategies.java) garde aussi de petits raccourcis de création, utilisés par les tests.

## 7. Les tournois

[Tournoi.java](src/main/java/fr/imt/arena/Tournoi.java) regroupe les informations d'une compétition : nom, jeu, date, budget, une à quatre arènes, inscriptions, matchs et points locaux.

[EtatTournoi.java](src/main/java/fr/imt/arena/EtatTournoi.java) décrit son évolution :

```text
PLANIFIE → EN_COURS → TERMINE
```

`inscrire` n'accepte les équipes que tant que le tournoi est planifié et refuse un nom déjà inscrit. `demarrer` demande au moins quatre équipes, des arbitres qualifiés fournis par Arena, un coach et assez de joueurs par équipe, et des équipes libres.

S'il échoue après avoir réservé certaines équipes, il les libère dans le `catch`. On ne laisse donc pas une équipe bloquée par un lancement refusé.

### Générer toutes les rencontres

La méthode `creerRencontres` utilise deux boucles :

```java
for (int i = 0; i < equipes.size(); i++) {
    for (int j = i + 1; j < equipes.size(); j++) {
        // Créer le match entre l'équipe i et l'équipe j.
    }
}
```

Pourquoi `j = i + 1` ? Pour ne pas créer de rencontre contre soi-même, ni rejouer BA après AB. Avec A, B, C, D, cela donne **AB, AC, AD, BC, BD, CD** : six matchs. La formule est `n × (n − 1) / 2`, donc quinze matchs pour six équipes.

L'arbitre est affecté à tour de rôle. L'opérateur `%` donne le reste d'une division ; `numeroMatch % nombreArbitres` permet de revenir au premier arbitre après le dernier.

### Points et fin du tournoi

`terminerMatch` résout la rencontre et ajoute trois points dans la map du tournoi si une équipe gagne. Ces points locaux sont séparés des points généraux d'Equipe.

`classement()` trie par points locaux, puis par nom. `matchsParVainqueur()` parcourt les équipes et les matchs pour construire les groupes de victoires. Les incidents n'y figurent pas.

`cloturer()` compte les matchs joués, choisit le premier du classement local, termine le tournoi, libère les équipes et crée le rapport. Avec zéro match joué, il n'y a pas de vainqueur.

### Consulter et attendre le résultat

`consulter()` construit une [VueTournoi](src/main/java/fr/imt/arena/VueTournoi.java). C'est une classe avec des champs finaux, une copie des points et une liste de [ResultatMatch](src/main/java/fr/imt/arena/ResultatMatch.java). Ces résultats contiennent des noms et des états copiés. Cette photographie ne change donc plus lorsque la simulation avance.

`attendreRapport(Duration.ofSeconds(10))` attend le rapport au maximum dix secondes. Le tournoi utilise pour cela un `CompletableFuture<Rapport>`, que l'on peut voir comme une boîte remplie plus tard par le worker :

- `publier` remplit la boîte avec le rapport après la sauvegarde et la libération des engagements.
- `echec` y place une erreur si la simulation ou le stockage a échoué.
- `get` attend son contenu pendant le délai demandé.

Un délai expiré produit `TimeoutException`, sans annuler la simulation. `ExecutionException` signale un échec du travail ; sa cause explique le problème. `InterruptedException` indique que le thread qui attend a été interrompu.

L'état `TERMINE` est fixé avant la sauvegarde : attendre le rapport est nécessaire pour confirmer que cette sauvegarde a abouti.

## 8. Arena et les collections

[Arena.java](src/main/java/fr/imt/arena/Arena.java) organise la saison. On lui fournit un dépôt de rapports dans son constructeur. Elle conserve les registres et décide quel tournoi lancer.

| Appel | Utilité |
|---|---|
| `ajouter(personne)`, `ajouter(jeu)`, `ajouter(equipe)` | Enregistrer les objets et refuser les doublons de clé. |
| `planifier(tournoi)` | Ajouter un tournoi encore planifié à la file. |
| `lancerSuivant(parametres, graine)` | Vérifier la capacité, démarrer le prochain et confier sa simulation au pool. |
| `equipe(nom)` | Retrouver une équipe à partir de son nom. |
| `planifies()` / `termines()` | Lister par date croissante / budget décroissant. |
| `classement()` | Classer les équipes enregistrées par points généraux, puis par nom. |
| `arbitres(true)` / `arbitres(false)` | Lire les arbitres disponibles / occupés. |
| `matchs(equipe)` | Lister les rencontres d'une équipe dans les tournois connus. |
| `nombreEnCours()` | Lire le nombre de tournois suivis comme actifs. |
| `observer(...)` / `erreursObservateurs()` | Ajouter un destinataire des résultats / lire ses éventuelles erreurs. |
| `close()` | Fermer le pool et attendre ses tâches. |

**Enregistrer une équipe et l'inscrire à un tournoi sont deux actions différentes.** Sans enregistrement dans Arena, elle n'apparaît pas au classement général du registre, même si elle participe à un tournoi.

### Quelle collection pour quel besoin ?

| Structure | Exemple dans le projet | Pourquoi ? |
|---|---|---|
| `ArrayList` | Joueurs, rencontres, observateurs | Une liste ordonnée, facile à parcourir et à compléter. |
| `HashMap` | Nom d'équipe → objet Equipe | Accéder directement à une valeur par sa clé. |
| `LinkedHashMap` | Personnes et groupes de gagnants | Comme une map, en conservant l'ordre d'ajout. |
| `HashSet` | Tournois en cours | Conserver un ensemble sans doublon. |
| `Set<Jeu>` | Qualifications | Chaque jeu apparaît une seule fois. |
| `PriorityQueue` | Tournois planifiés | Retrouver le plus proche avec `peek`, puis le retirer avec `remove`. |
| `ConcurrentLinkedQueue` | Erreurs d'observateurs | Recevoir des erreurs depuis plusieurs threads en sécurité. |

Une recherche dans une HashMap coûte en moyenne `O(1)` ; ajouter ou retirer la tête d'une PriorityQueue coûte `O(log n)` ; trier une liste coûte `O(n log n)`. Ces notations décrivent la croissance du travail quand le nombre d'éléments augmente, pas des temps précis.

`Tournoi.compareTo` donne l'ordre chronologique, puis le nom en cas d'égalité. `resultat.sort(null)` demande cet ordre naturel. Les listes par budget ou par points utilisent un `Comparator`.

Dans `new Comparator<Equipe>() { ... }`, le bloc crée un comparateur et définit directement sa méthode `compare`. C'est une **classe anonyme**. La méthode renvoie une valeur négative si le premier élément doit passer avant l'autre, zéro s'ils sont équivalents, une valeur positive sinon. `Integer.compare` compare les entiers sans risquer de dépasser leur capacité avec une soustraction.

Arena copie les scores avant le tri, car ils peuvent changer pendant une simulation. On ne garde pas un TreeSet trié par points : modifier les points ne réorganiserait pas automatiquement son arbre. La copie stabilise le tri ; elle ne constitue pas une photographie simultanée de tous les objets de la saison.

## 9. La simulation et les threads

### Un fil principal et trois fils de travail

Un **thread** est un fil d'exécution. Le thread principal peut afficher les informations tandis qu'un autre fait avancer un tournoi. `Executors.newFixedThreadPool(3)` crée un pool capable d'exécuter trois tâches simultanées.

Arena envoie une tâche par tournoi, pas une tâche par joueur ou par match. La seule lambda du code de production sert à cet envoi :

```java
executeur.execute(() -> simuler(tournoi, qualifies, parametres, graine, abonnes));
```

`()` signifie qu'il n'y a pas de paramètre à fournir à la tâche ; après `->`, on indique le travail à exécuter. Cette ligne demande au pool « exécute cet appel sur un worker ». L'appelant continue, sauf s'il choisit ensuite d'attendre le rapport.

### Pourquoi `synchronized` ?

Sans protection, deux appels pourraient lire « deux tournois actifs », lancer chacun un tournoi et arriver à quatre. La méthode `lancerSuivant` est synchronisée : contrôle de capacité, choix, réservations et soumission sont effectués sous le même verrou d'Arena. Le second appel voit donc le résultat du premier.

Un pool de trois threads ne suffit pas à faire respecter cette règle : il pourrait mettre un quatrième travail en attente. Le contrôle explicite du nombre de tournois reste nécessaire.

Chaque équipe protège aussi ses membres et sa réservation. Les personnes protègent leurs compteurs ; le tournoi protège ses résultats et la création de ses vues. Le verrou rend les écritures visibles pour les lectures protégées par le même verrou.

Les sommeils, les notifications et l'écriture H2 se font sans garder le verrou d'Arena. `close()` ne le garde pas non plus pendant son attente : les workers en ont besoin pour terminer proprement.

### Paramètres et durée

[ParametresSimulation.java](src/main/java/fr/imt/arena/ParametresSimulation.java) contient trois valeurs : durée par match, probabilité d'incident et stratégie. Il refuse une durée négative, une probabilité invalide ou une stratégie absente. Une durée nulle est autorisée pour les tests.

[Simulateur.java](src/main/java/fr/imt/arena/Simulateur.java) effectue ces étapes :

1. Lire les rencontres et mémoriser l'instant de départ avec `System.nanoTime()`.
2. Calculer la durée cible : `nombreMatchs × tempsParMatch / nombreArenes`.
3. Parcourir les rencontres par lots d'au plus `nombreArenes` matchs.
4. Attendre l'échéance du lot, en tenant compte du temps déjà écoulé.
5. Tirer un incident avec `random.nextDouble() < probabiliteIncident`.
6. Résoudre le match, puis avertir les observateurs.

Avec quinze matchs à 100 ms et trois arènes, cinq lots donnent **500 ms**. Le modèle utilise des arènes logiques, pas un thread par arène. Si le dernier lot est incomplet, il est raccourci pour conserver la formule du sujet : avec quatre arènes, la cible vaut 375 ms. Les calculs et la sauvegarde ajoutent éventuellement du temps réel.

Chaque tournoi reçoit son propre `new Random(graine)`. Avec des équipes neuves, les mêmes paramètres, la même stratégie et la même graine donnent les mêmes tirages. Cela rend les tests reproductibles.

### Avertir d'autres objets : Observer

[EcouteurMatch.java](src/main/java/fr/imt/arena/EcouteurMatch.java) demande une méthode `matchTermine`. [CompteurMatchs.java](src/main/java/fr/imt/arena/CompteurMatchs.java) en est un exemple concret : il compte chaque notification avec une méthode synchronisée.

```java
CompteurMatchs compteur = new CompteurMatchs();
arena.observer(compteur);
```

Le simulateur avertit les objets abonnés après un match joué ou annulé. C'est le pattern **Observer**. Match n'a pas à connaître une console ou un journal particulier. Les points restent mis à jour directement par le modèle : ils ne dépendent pas d'un observateur facultatif.

La liste des abonnés est copiée au lancement. Un abonnement ajouté ensuite servira aux prochains tournois. Les callbacks sont synchrones : ils s'exécutent dans le worker et doivent terminer rapidement. Une `RuntimeException` d'un observateur est conservée dans la file d'erreurs sans empêcher les autres notifications.

### Fermer même en cas de problème

`Arena.simuler` annule les rencontres restantes en cas d'échec du déroulement, clôture et tente d'enregistrer le rapport. Un `finally` libère les engagements des arbitres et retire le tournoi de la liste active. Les équipes sont libérées lors de la clôture. L'erreur est transmise à l'attente du rapport.

`close()` interdit les nouveaux lancements, attend jusqu'à trente secondes, puis demande l'interruption des workers si nécessaire et attend encore jusqu'à dix secondes. Une interruption est une demande d'arrêt : Java ne peut pas forcer proprement une extension arbitraire qui refuse de terminer. Les stratégies et observateurs doivent coopérer et ne pas appeler `Arena.close()` depuis leur worker, qui attendrait alors sa propre fin.

## 10. Les rapports et la base de données

### Qui fait quoi ?

- **JPA** définit les règles pour relier des objets Java à une base.
- **Hibernate** est l'outil qui applique ces règles dans ce projet.
- **H2** est le moteur qui enregistre les lignes dans un fichier.

[Rapport.java](src/main/java/fr/imt/arena/Rapport.java) est le seul objet enregistré. Il garde huit informations : identifiant, nom du tournoi, date, jeu, matchs prévus, matchs joués, nom du vainqueur, réussite. Les équipes et joueurs sont recréés en mémoire lors d'une nouvelle exécution ; seuls les rapports survivent.

`@Entity` désigne une classe persistante ; `@Id` désigne sa clé ; `@GeneratedValue` laisse la base attribuer l'identifiant. `@Column(nullable = false)` interdit une valeur SQL nulle. Le constructeur vide protégé permet à Hibernate de reconstruire un rapport.

La réussite est calculée sans arrondi :

```java
return joues * 10L >= prevus * 7L;
```

Cela signifie « au moins 70 % des matchs prévus ont été joués ». Le `L` demande des calculs en `long`. Sept sur dix suffisent ; quatre sur six ne suffisent pas ; cinq sur six suffisent.

### Le dépôt

[DepotRapports.java](src/main/java/fr/imt/arena/DepotRapports.java) annonce trois opérations : `sauvegarder`, `tous`, `parJeu`. Arena demande ces opérations sans connaître les détails du stockage. Les tests peuvent fournir un dépôt mémoire à la place de H2.

[DepotJpa.java](src/main/java/fr/imt/arena/DepotJpa.java) implémente ces opérations avec JPA. La `EntityManagerFactory` prépare les accès à la base. Chaque opération crée son propre `EntityManager` ; cet objet ne doit pas être partagé entre workers.

Une sauvegarde ouvre une transaction avec `begin`, demande l'enregistrement avec `persist`, puis valide avec `commit`. En cas d'erreur, `rollback` annule la transaction active et l'exception remonte. Le try-with-resources ferme l'EntityManager.

`tous` et `parJeu` utilisent des requêtes JPQL, portant sur la classe Rapport. Le filtre `:jeu` est un paramètre rempli séparément, pas du texte collé dans la requête. Les résultats sont retournés dans une liste non modifiable.

### Où se trouve le fichier ?

[persistence.xml](src/main/resources/META-INF/persistence.xml) configure l'unité `arena`, Hibernate et l'URL `jdbc:h2:file:./data/arena`. Le fichier est **`data/arena.mv.db`**, relatif au dossier depuis lequel on lance la commande. H2 crée le dossier nécessaire. Il n'y a pas de serveur séparé ni de clé à fournir.

Le réglage `hibernate.hbm2ddl.auto = update` crée ou adapte les tables au démarrage ; il suffit pour ce TP, sans remplacer un système de migrations de production. Les tests fournissent d'autres URL, dans des dossiers temporaires.

Ne pas ouvrir deux JVM simultanément sur ce même fichier. Pour effacer les rapports, fermer l'application puis supprimer `data/` ; cette suppression est définitive. Une sauvegarde échouée est signalée par `attendreRapport`, jamais présentée comme réussie.

## 11. Ce que fait Main

[Main.java](src/main/java/fr/imt/arena/Main.java) contient une démonstration complète. Ses petites méthodes d'aide créent les données et affichent les listes.

1. Ouvrir DepotJpa puis Arena.
2. Créer deux jeux et deux arbitres qualifiés pour ces jeux.
3. Créer six équipes, avec un coach et trois amateurs chacune, et les enregistrer.
4. Inscrire ces équipes à deux tournois ayant des dates et des budgets différents.
5. Planifier les tournois dans l'ordre inverse de leurs dates pour montrer la priorité chronologique.
6. Ajouter CompteurMatchs, choisir 100 ms par match, 10 % d'incidents et StrategieEquitable.
7. Lancer le premier tournoi avec la graine 42, afficher son état et attendre son rapport.
8. Afficher son classement local et les groupes de matchs gagnés.
9. Lancer le second avec la graine 84 après la libération des mêmes équipes.
10. Afficher le classement général, les arbitres disponibles, les matchs d'une équipe et les rapports enregistrés.

Le Main montre des tournois successifs parce qu'ils partagent les équipes. Les tests de concurrence utilisent des équipes différentes pour démontrer trois tournois actifs ensemble.

Les ressources sont déclarées dans un try-with-resources :

```java
try (DepotJpa depot = new DepotJpa(); Arena arena = new Arena(depot)) {
    // Créer les données, lancer les tournois et consulter les résultats.
}
```

Java les ferme dans l'ordre inverse : **Arena d'abord, DepotJpa ensuite**. Les threads terminent ainsi leurs sauvegardes avant la fermeture de la base. Les `System.out.println` restent dans Main, y compris ses méthodes d'affichage.

## 12. Les tests

Les tests suivent trois étapes : **préparer des objets → appeler une méthode → vérifier le résultat**. Les 42 exécutions des cinq suites existantes ont été rejouées après simplification, sans modifier leurs assertions.

| Fichier | Ce qu'il vérifie |
|---|---|
| [DomainTest](src/test/java/fr/imt/arena/DomainTest.java) | Appartenance, transferts, promotions, coachs, jeux, qualifications, points et stratégies. |
| [TournamentTest](src/test/java/fr/imt/arena/TournamentTest.java) | Préconditions, génération des rencontres, priorité, budget, registres et rollback. |
| [SimulationTest](src/test/java/fr/imt/arena/SimulationTest.java) | Incidents, seuil de 70 %, tirages reproductibles, observateurs, durée, rapports et erreurs. |
| [ConcurrencyTest](src/test/java/fr/imt/arena/ConcurrencyTest.java) | Trois lancements au maximum, réservations, mutations, fermeture et vues cohérentes. |
| [PersistenceTest](src/test/java/fr/imt/arena/PersistenceTest.java) | Création de fichiers H2, sauvegardes concurrentes et relecture après fermeture/réouverture. |
| [Fixtures](src/test/java/fr/imt/arena/Fixtures.java) | Fournit les données communes et le dépôt mémoire aux tests. Ce n'est pas une suite de tests. |

`@BeforeEach` prépare des objets neufs ; `@AfterEach` ferme les ressources ; `@ParameterizedTest` rejoue la même règle sur plusieurs valeurs. `assertEquals` compare des valeurs, `assertSame` compare les références et `assertThrows` vérifie qu'une opération invalide produit l'exception attendue.

Les tests gardent quelques lambdas courantes pour les assertions et les appels concurrents. Par exemple, `assertThrows(..., () -> b.ajouter(joueur))` donne à JUnit l'action qu'il doit tenter. Les tests de concurrence utilisent des barrières (`CountDownLatch`) et des attentes limitées, afin de maîtriser la situation au lieu de compter sur la chance.

Pour lancer une seule suite :

```sh
mvn -Dtest=DomainTest test
mvn -Dtest=ConcurrencyTest test
```

Les résultats détaillés se trouvent dans `target/surefire-reports/`. Ces tests protègent les règles importantes ; ils ne constituent pas une preuve mathématique de l'absence de toute erreur possible.

## 13. Comment construire ce projet soi-même

Avancer dans cet ordre permet de comprendre chaque ajout :

1. Écrire les règles du sujet avec un exemple valide et un exemple refusé.
2. Dessiner Personne, ses trois sous-classes, Jeu, Equipe, Match et Tournoi.
3. Créer le projet Maven et commencer par les personnes et les équipes.
4. Tester l'interdiction de double appartenance et la promotion après trois victoires.
5. Ajouter les jeux, les compositions et le résultat d'un match.
6. Ajouter les inscriptions, les états et la double boucle de rencontres du tournoi.
7. Ajouter Arena, ses registres, sa file de priorité et les réservations.
8. Ajouter le temps, le hasard et les threads, puis vérifier les lancements concurrents.
9. Brancher le stockage via DepotRapports et tester la relecture d'un fichier fermé.
10. Terminer par Main, les explications, l'UML et le ZIP.

Pour ajouter une règle de victoire, créer une classe qui implémente StrategieVainqueur, écrire `choisir`, puis passer cet objet à ParametresSimulation. Pour ajouter un type de jeu, étendre Jeu. Pour recevoir les résultats, implémenter EcouteurMatch. Il n'est pas nécessaire de modifier toutes les classes existantes.

Pour l'oral, suivre ce parcours dans les fichiers :

```text
Main
  → Arena.lancerSuivant
  → Tournoi.demarrer et creerRencontres
  → Simulateur.executer
  → Tournoi.terminerMatch
  → Match.terminer
  → Equipe.victoire
  → Tournoi.cloturer
  → DepotJpa.sauvegarder
  → récupération du rapport par Main
```

À chaque étape, poser trois questions : **qui possède la donnée ? qui peut la modifier ? quelle règle est vérifiée ?**

## 14. Les fichiers techniques et les choix du sujet

[pom.xml](pom.xml) fixe Java 21, JUnit 5, Hibernate et H2. Les plugins Maven compilent, exécutent les tests et lancent Main. `target/` est généré ; `.gitignore` exclut ce dossier, les bases et les logs. `.gitattributes` déclare les PDF, PNG et ZIP comme fichiers binaires.

[mvn-cloud](mvn-cloud) et [scripts/cloud_setup.sh](scripts/cloud_setup.sh) servent au cloud : JDK complet, Maven, cache inscriptible et proxy. Les téléchargements de l'installation sont vérifiés par leurs empreintes officielles. [scripts/generer_dossier.py](scripts/generer_dossier.py) génère le PDF avec Python, ReportLab, Graphviz et les polices Open Sans ; ces outils sont facultatifs pour développer en Java, car le dossier est déjà fourni.

Les interprétations suivantes sont explicites dans le projet :

- Une seule Arena coordonne les tournois d'une saison ; les identifiants des personnes sont uniques.
- Le nom nettoyé d'un jeu est son identité, avec une distinction entre majuscules et minuscules.
- La date ordonne les tournois, sans attendre la date réelle pour lancer une simulation.
- Les points généraux et les victoires du coach sont cumulatifs ; le classement d'un tournoi utilise ses points locaux.
- Les égalités sont départagées par nom ; sans match joué, il n'y a pas de vainqueur.
- Une victoire fait progresser tous les joueurs de l'effectif, même non sélectionnés ; une promotion est conservée après transfert.
- Les arènes sont logiques, avec dernier lot proratisé. On n'ajoute pas de planning exclusif des équipes ou arbitres par rencontre.
- Un arbitre occupé est engagé dans au moins un tournoi. Les matchs annulés ne comptent pas comme arbitrés.
- Les stratégies et observateurs personnalisés doivent terminer rapidement et respecter les interruptions.

Le dossier de conception reste limité à **deux pages**. Le README, l'analyse, la validation et les questions d'oral sont des documents séparés. Le ZIP contient les sources, les tests et les documents ; il exclut Git, les caches, les sorties de compilation et la base de démonstration.
