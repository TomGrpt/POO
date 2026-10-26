# Vérification exécutée — 26 octobre 2026

## Environnement et commandes

JDK Temurin 21.0.9+10 complet et Maven 3.9.11, téléchargés via HTTPS et vérifiés par SHA-256 / SHA-512 officiels. Le Java préinstallé était incomplet (`ct.sym` absent) : le JDK autonome le remplace pour Maven, sans modifier les bibliothèques du système. Cache et configuration proxy hors checkout. Aucun contournement TLS, de checksum ou d'assertion.

Depuis `/workspace/POO` :

```sh
./mvn-cloud -B clean verify
./mvn-cloud -B exec:java
```

Résultat : **42 exécutions de tests, 0 échec, 0 erreur, 0 ignoré**, jar Maven produit. Répartition : DomainTest 15, TournamentTest 8, SimulationTest 11, ConcurrencyTest 6, PersistenceTest 2.

## Observations fonctionnelles

La démonstration réelle termine normalement : deux tournois de 15 matchs, 14 matchs joués chacun ; vainqueurs « Equipe 1 » (Duel), « Equipe 3 » (Arena 3). Deux rapports sont automatiquement enregistrés, trente notifications Observer reçues. Deux arbitres apparaissent occupés au lancement, puis disponibles après clôture. Le classement général totalise 84 points, soit 28 victoires × 3. Dix rencontres concernent l'équipe 1 sur les deux tournois.

Les tests de persistance créent de vrais fichiers H2 temporaires, ferment et rouvrent les factories ; identifiants, date, jeu, comptes, vainqueur et réussite sont relus. Un second test sauvegarde depuis trois workers et retrouve trois rapports après réouverture.

La concurrence est testée avec latches et délais bornés : huit appels de lancement, exactement trois acceptés tant que les tâches sont retenues ; double réservation refusée ; effectifs immuables pendant engagement ; deux équipes concurrentes ne peuvent accueillir le même joueur ; fermeture normale et fermeture sur interruption libèrent les ressources. Les photographies de tournoi présentent toujours un total de points égal à trois fois le nombre de matchs joués. Même graine : même liste de résultats.

Le calcul de durée est vérifié sur 15×100/3=500 ms et 15×100/4=375 ms. Une simulation réelle du premier cas ne se termine pas prématurément ; le test tolère les surcoûts du système, sans imposer une borne supérieure fragile autre que l'attente maximale. Un délai de rapport trop court déclenche TimeoutException.

Les erreurs de stratégie et de sauvegarde sont visibles via ExecutionException ; les réservations sont libérées. Les exceptions d'observateur sont collectées et n'empêchent pas les autres notifications. Une interruption annule les rencontres restantes, génère un rapport incomplet et signale l'interruption à l'appelant.

## Interprétations et limites explicites

- Modèle de temps agrégé avec arènes logiques, dernier lot proratisé ; pas de planning sportif excluant simultanément une équipe ou un arbitre. Ce choix suit la formule imposée et son exemple.
- Disponibilité d'un arbitre = aucun engagement dans un tournoi. Les arbitres peuvent être partagés, leur exclusivité n'étant pas imposée.
- Classement local du tournoi, classement général cumulatif ; égalités départagées par nom. Aucun vainqueur si tous les matchs sont annulés.
- Identifiants uniques supposés par le PDF ; une seule Arena coordonne les tournois d'une saison. Les extensions utilisateur doivent terminer et coopérer avec l'interruption : Java ne peut forcer l'arrêt d'un callback arbitraire.
- Aucun test n'est présenté comme une preuve mathématique d'absence de toute course. Pas de couverture chiffrée inventée ; les tests visent les règles critiques.
- Aucun dépôt distant modifié, commit ou push effectué. Les sorties générées et bases de démonstration sont ignorées et exclues de l'archive. Le PDF officiel est une entrée de travail, pas un livrable réédité.

La préparation à l'oral est fournie ; l'assiduité et les réponses lors de l'entretien ne peuvent pas être validées par le logiciel.

## Contrôle de livraison

Le PDF a été généré, ses deux pages rendues et inspectées visuellement ; les polices sont intégrées. La source Mermaid et la représentation Graphviz correspondent aux classes livrées. L'inspection des sources confirme que les seuls appels System.out sont dans Main.

L'archive a été testée (intégrité ZIP et présence des livrables), extraite dans `/tmp/arena-livraison/POO`, puis reconstruite avec `mvn -o -B clean verify` via le lanceur cloud : **42 tests exécutés, 0 échec, 0 erreur, 0 ignoré, BUILD SUCCESS**. Aucun fichier source ou ressource ne dépend du répertoire de compilation initial. Seule cette note de contrôle a été ajoutée ensuite à la documentation. L'archive finale a été régénérée et son intégrité revérifiée.

Le script `scripts/cloud_setup.sh` a également été exécuté avec succès après installation des outils : il est répétable, et lance verify. `install_script` et `start_skill` ont été enregistrés dans le brouillon de configuration cloud ; cela ne constitue ni une publication ni une validation dans une nouvelle tâche. Pour les conserver comme environnement publié, revoir et enregistrer les changements dans les paramètres, puis publier l'environnement. Aucun processus de service n'est requis pour cette application console.
