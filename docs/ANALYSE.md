# Analyse et architecture (avant implémentation)

Le PDF est la spécification métier ; le texte de mission impose Java 21/Maven/JUnit/JPA.

## Exigences et décisions
Personne abstraite : Joueur (statut, victoires depuis arrivée), Coach (victoires carrière), Arbitre (qualifications, matchs arbitrés). Une Equipe encapsule ses membres, ses points et sa réservation. Jeu abstrait définit la sélection d'une composition ; Solo et Collectif sont polymorphes. Match conserve équipes, compositions, arbitre, résultat ou incident. Tournoi conserve les paramètres immuables, inscriptions figées au lancement, états et résultat asynchrone. Arena gère les registres et l'ordonnancement. Simulateur exécute un tournoi, applique une Strategy et notifie les observateurs. Rapport est la seule entité JPA ; DepotRapports abstrait le stockage.

Les inscriptions et les mutations d'effectif sont interdites pendant une réservation. Une réservation atomique par équipe protège également ses joueurs et son coach ; les appartenances exclusives rendent impossible leur présence dans deux équipes. Un lancement réserve toutes les équipes, avec rollback si une réservation échoue. Le verrou court de l'Arena protège file, limite de trois et lancement. Les équipes ont leur propre moniteur ; aucun verrou d'équipe n'est tenu durant une attente ou un accès JPA. L'identité des jeux est leur nom normalisé (même nom = même définition, garanti par registre). Les arbitres peuvent encadrer plusieurs rencontres logiques ; leur disponibilité signifie engagement dans un tournoi, pas exclusivité d'une personne par arène. Leur compteur compte uniquement les matchs effectivement joués.

Le PDF demande simultanément la formule exacte M*t/A et une durée par match. Pour un nombre non divisible d'arènes, le dernier lot est proratisé : temps simulé agrégé, et non planning sportif réel. Les rencontres sont traitées par lots d'au plus A et les résultats deviennent visibles à la fin du lot ; M=15,t=100,A=3 donne 500 ms, hors coût CPU/JPA. Il n'est pas ajouté d'exclusion entre rencontres d'une même équipe. Random est propre à chaque tournoi (graine injectée), ordre stable des rencontres et arbitres. Les classements de tournoi utilisent les points locaux ; les points d'équipe et victoires carrière restent cumulatifs. Les ex aequo sont départagés par nom. Si aucun match n'est joué, le rapport n'a pas de vainqueur. 70 % se calcule sans arrondi : joués*10 >= prévus*7. Un incident ne compte pas comme match arbitré. La promotion est acquise définitivement ; les victoires depuis arrivée repartent à zéro au changement d'équipe.

## Matrice de traçabilité prévue
| Exigence PDF | Implantation prévue | Validation prévue |
|---|---|---|
| Personnes, appartenance exclusive, promotion, carrière | Personne/Joueur/Coach/Equipe | DomainTest |
| Jeux solo/3–5, manches, extension | Jeu/JeuSolo/JeuCollectif | DomainTest |
| Match distinct, qualification, 3 points, incident | Match/Equipe | DomainTest |
| Nom/date/budget/1–4 arènes, inscriptions | Tournoi | TournamentTest |
| >=4 équipes, coach/effectif/arbitre | Tournoi/Arena | TournamentTest |
| Réservations équipe/joueur/coach, mutations | Equipe/Arena | ConcurrencyTest |
| File chronologique, max 3 atomique | Arena/PriorityQueue | ConcurrencyTest |
| Round robin M=n(n−1)/2 | Tournoi | TournamentTest |
| Durée M*t/A, hasard, incidents, clôture 70 % | Simulateur/Tournoi | SimulationTest |
| Rapport avec attente bornée, arrêt propre | Tournoi/Arena | SimulationTest/ConcurrencyTest |
| Listes planifiés/terminés/classement/arbitres | Arena/Classe | TournamentTest/Main |
| Matchs équipe et regroupement vainqueur | Arena/Tournoi | SimulationTest/Main |
| Strategy/Observer, collections, Comparable/Comparator | Strategies/EcouteurMatch/Arena | DomainTest/SimulationTest |
| Rapports seuls JPA H2 fichier, tous/par jeu | Rapport/DepotJpa | PersistenceTest |
| Démonstration sans sorties métier | Main | exécution réelle |
| UML/dossier <=2 pages/README/ZIP | docs et archive | inspection finale |

Factory et Builder non retenus : constructeurs validants courts, sans assemblage complexe. Strategy et Observer ont un usage réel. Critères : réalisation 8, échanges 6, assiduité 2 (hors logiciel), qualité 4. Préparation orale distincte du dossier limité à deux pages.

## Traçabilité finale vérifiée

| Référence / obligation | Classes et méthodes réelles | Test ou preuve exécutée |
|---|---|---|
| §1 Personnes : six propriétés communes | `Personne`, constructeur et accesseurs immuables | Compilation ; création des trois catégories dans Main et tests |
| §1 Joueur : une équipe, statut et promotion | `Equipe.ajouter/retirer`, `Joueur.rejoindre/victoire` | `appartenanceExclusiveEtTransfert`, `promotionApresTroisVictoiresEtResetArrivee`, `deuxEquipesConvoitentLeMemeJoueur` |
| §1 Coach exclusif, victoires carrière | `Equipe.nommer`, `Coach.rejoindre/victoire` | `coachExclusifEtCarriere` |
| §1 Arbitre : qualifications et compteur | `Arbitre.estQualifie/matchJoue`, `Match.terminer` | `qualificationEtMatchContreSoi`, `victoireEtAnnulation` |
| §1 Jeux : nom/manches, solo et 3–5, extensibilité | `Jeu`, `JeuSolo`, `JeuCollectif`, `selectionner` | `compositionsCollectives`, `taillesInvalides`, `selectionSoloEtExtensionPolymorphe` |
| §1 Match : adversaires distincts, jeu/arbitre/vainqueur | `Match` constructeur, `terminer` | `qualificationEtMatchContreSoi`, `strategiesEtVainqueurInvalide` |
| §1 Points et absence de nul | `Equipe.victoire`, `Match.terminer` | `victoireEtAnnulation`, `reproductibiliteEtGroupement` |
| §1 Tournoi : paramètres et 1–4 arènes | `Tournoi` constructeur | `parametresInvalidesEtDoublons`, `roundRobin` ; Main |
| §1 Toutes rondes | `Tournoi.demarrer` double boucle i<j | `roundRobin` paramétré 4/5/6 : nombres, unicité, rencontres par équipe |
| §1 Conditions de démarrage | `Tournoi.demarrer`, `Equipe.reserver/verifier`, `Arena.lancerSuivant` | `minimumEquipes`, `coachEtEffectifObligatoiresRollback`, `arbitreObligatoire` |
| §1 Aucune double mobilisation | `Equipe.reserver`, mutations contrôlées, appartenances exclusives | `pasDeDoubleReservationEtPasDeMutation`, `deuxEquipesConvoitentLeMemeJoueur` |
| §1 Priorité temporelle et maximum 3 | `Tournoi.compareTo`, `Arena.planifier/lancerSuivant` | `ordreChronologiqueBudgetEtRegistre`, `troisLancementsMaximumSousConcurrence` |
| §1/2 Etats, fin automatique, réussite >=70% | `Tournoi.cloturer`, `Rapport.estReussi`, `Arena.simuler` | `seuilSansArrondi`, `incidentsEtObservateursIsoles`, `reproductibiliteEtGroupement` |
| §2 Thread séparé, durée M*t/A et exemple | `Arena.executeur`, `Simulateur.executer`, `ParametresSimulation.dureeNanos` | `formuleEtDureeReelle`, tests concurrence ; Main utilisable au lancement |
| §2 Arbitre qualifié et hasard/incident | `Tournoi.demarrer`, `Simulateur.executer`, `Strategies` | `qualificationEtMatchContreSoi`, `incidentsEtObservateursIsoles`, `reproductibiliteEtGroupement` |
| §2 Consultation cohérente, rapport borné | `Tournoi.consulter/attendreRapport`, `VueTournoi` | `photographieCoherentePendantLaSimulation`, `formuleEtDureeReelle` |
| §2 Arrêt propre et erreurs | `Arena.close/simuler` | `fermetureAttendLaFinEtInterditLesLancements`, `interruptionFermetureArreteLesWorkersEtRestaureLeFlag`, erreurs stockage/stratégie |
| §3 Ajouter personnes/jeux/équipes/tournois | `Arena.ajouter/planifier` | Main exécuté, `ordreChronologiqueBudgetEtRegistre` |
| §3 Listes chronologique, budget, classement, accès nom | `Arena.planifies/termines/classement/equipe` | `ordreChronologiqueBudgetEtRegistre` ; Main : budgets 2000 avant 1000, classement affiché |
| §3 Arbitres disponibles/occupés | `Arbitre.engager/liberer/disponible`, `Arena.arbitres` | `reproductibiliteEtGroupement` ; Main : 2 occupés puis 2 disponibles |
| §3 Matchs équipe et groupes gagnants | `Arena.matchs`, `Tournoi.matchsParVainqueur` | `reproductibiliteEtGroupement` ; Main : 10 matchs équipe 1, gagnants et tailles affichés |
| §4 Héritage, interfaces, encapsulation | `Personne`, `Jeu`, `Qualifie`, `Classe`, collections copiées | Tests domaine, `selectionSoloEtExtensionPolymorphe`, tests collections immuables |
| §4 Collections, Comparable/Comparator | Queue, maps, sets, listes ; `Tournoi.compareTo`, `Classe.CLASSEMENT` | Tests ordre/classement, dossier page 2 |
| §4 Patterns pertinents | `StrategieVainqueur`, `Strategies`, `EcouteurMatch` | `strategiesEtVainqueurInvalide`, `incidentsEtObservateursIsoles` ; Main : 30 notifications |
| §4 Rapports seuls JPA H2 fichier, huit données | `Rapport`, `DepotJpa`, `persistence.xml` | `sauvegardeAutomatiqueEtReouvertureFichier`, `troisSauvegardesDepuisLesWorkers` |
| §4 Sauvegarde automatique / tous / par jeu | `Arena.simuler`, `DepotJpa.sauvegarder/tous/parJeu` | Les deux tests de persistance ; `echecPersistanceObservableMaisReservationsLiberees` |
| §4 >=3 tests JUnit diversifiés | Cinq suites, paramétrés, assertThrows, BeforeEach/AfterEach | 42 exécutions, aucune ignorée : `VALIDATION.md` |
| §4/5 Main, Random, sorties réservées | `Main`, `new Random(graine)` par tournoi | Démonstration exécutée ; inspection des sources |
| §6 Rendu ZIP, README, conception <=2 pages | Archive, README, `DOSSIER_CONCEPTION.pdf`, Mermaid | Nombre de pages et contenu de l'archive vérifiés ; validation détaillée ci-dessous |

Le dossier de conception limité à deux pages est le PDF désigné. L'analyse, la matrice de traçabilité, la validation et la préparation orale sont des annexes séparées, demandées par la mission.
