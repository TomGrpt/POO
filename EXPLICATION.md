# Comprendre la nouvelle organisation

## 1. Les dossiers et les imports

Un package regroupe des classes qui travaillent sur le même sujet. Son nom correspond au chemin des dossiers. Dans `jeux/Jeu.java`, on trouve donc `package fr.imt.arena.jeux;`.

Quand Main crée une Equipe, il importe `fr.imt.arena.metier.Equipe`. L'import indique où trouver la classe ; il ne crée pas d'objet.

Les classes métier restent ensemble pour que leurs méthodes internes restent accessibles à leurs partenaires, sans devenir des opérations publiques. Le constructeur de RapportTournoi est public pour permettre sa création depuis `metier` ; il vérifie le nombre de matchs joués. Les autres opérations internes conservent leur visibilité.

## 2. Les personnes et les équipes

Personne contient l'identité commune. Joueur, Coach et Arbitre héritent de ces données. Equipe contient les joueurs, un coach, les points et son état d'occupation.

`ajouterJoueur` vérifie que les équipes sont libres, retire éventuellement le joueur de son ancienne équipe, puis l'affecte à la nouvelle. Les deux côtés changent dans le même bloc protégé. `definirCoach` suit le même principe.

Le joueur devient professionnel après trois victoires depuis son arrivée. Le coach conserve son total de carrière après un transfert. Ces compteurs n'ont pas de setters publics.

## 3. Les jeux

JeuSolo sélectionne un joueur. JeuEquipe en sélectionne trois à cinq. La classe Jeu sélectionne les premiers joueurs à l'aide d'une boucle.

Le gestionnaire utilise l'interface commune de Jeu sans avoir besoin de tester tous les types de jeux. C'est le polymorphisme : la méthode appelée dépend de l'objet réel.

## 4. Le vainqueur et les rencontres

StrategieVictoire décrit la méthode `choisir`. TirageEquitable utilise une condition sur un booléen aléatoire ; MeilleureEquipe compare les points puis le nom. Changer l'objet stratégie change la règle de victoire.

Les rencontres sont générées avec `i`, puis `j` qui commence à `i + 1`. Cela évite les matchs contre soi et les doublons AB/BA. Quatre équipes donnent six rencontres, six équipes donnent quinze rencontres.

Match est un résultat immuable créé par la simulation. Un vainqueur nul signifie un incident ou une interruption. Il n'y a pas de résultat nul au sens sportif.

## 5. Planifier puis lancer

GestionnaireTournois conserve les objets enregistrés et une file de tournois planifiés. La PriorityQueue choisit la date la plus proche. Son parcours n'est pas forcément trié : la méthode d'affichage trie une copie.

Avant de lancer, le gestionnaire vérifie quatre équipes au minimum, leur enregistrement, les coachs, les joueurs, les effectifs et leur disponibilité. Il choisit ensuite un arbitre libre et qualifié. Toutes les vérifications et réservations se font sous le verrou commun.

## 6. Exécuter sans bloquer Main

Le pool contient trois threads de travail. FutureTask représente une tâche et sa valeur future. Le compteur `enCours` interdit une quatrième admission : le pool seul pourrait accepter une tâche supplémentaire en attente.

La lambda `() -> simuler(...)` décrit le travail confié au pool. Le Main peut continuer à consulter l'état. `attendreRapport` attend la valeur future avec un délai maximal, hors verrou.

La simulation utilise une graine fixe, attend selon la cadence, tire les incidents et produit les résultats. Les pauses et les stratégies se font hors verrou. Les points sont appliqués à la fin : la stratégie lit donc les points d'avant le tournoi.

## 7. Publier les résultats

Le gestionnaire reprend le verrou, applique les victoires, calcule les points locaux et crée le rapport. Il sauvegarde puis publie ensemble les matchs, le rapport et l'état terminé. Les réservations sont libérées même si la sauvegarde échoue.

Ce verrou commun rend les transferts et la publication faciles à suivre. En contrepartie, une sauvegarde lente peut retarder les autres opérations protégées. Cette version conserve ce choix du ZIP pour éviter une architecture plus compliquée.

## 8. Enregistrer les rapports

RapportTournoi est annoté `@Entity` : Hibernate peut l'enregistrer avec JPA. H2 conserve les données dans un fichier. Seuls les rapports sont persistés.

DepotRapports crée un EntityManager pour chaque opération. `begin` démarre la transaction, `persist` demande l'enregistrement et `commit` valide. Avec une erreur, `rollback` annule la transaction active. Le try-with-resources ferme l'EntityManager.

Le programme ferme le gestionnaire avant le dépôt : les threads doivent finir leurs sauvegardes avant la fermeture de la base.

## 9. Les simplifications effectuées

- Des dossiers par responsabilité.
- Des getters écrits sur plusieurs lignes.
- Des accolades pour les conditions et les boucles.
- Des comparateurs explicites pour les tris.
- Des étapes séparées pour les requêtes JPA.
- Des conditions simples pour les stratégies.
- Des boucles pour sélectionner les joueurs et regrouper les victoires.
- Des commentaires de 1 à 5 mots.

Les assertions des tests existants n'ont pas été supprimées. Les imports et le package des tests suivent la nouvelle arborescence.
