# Trois programmes séparés

Ouvrir pom.xml comme projet Maven, avec Java 17 ou supérieur.

Dans src/main/java/fr/imt/arena/application/ :
- MainSequentiel.java : un tournoi après l’autre.
- MainMultithread.java : un thread dédié par tournoi.
- MainPool.java : un pool de trois threads.

Lancer le main du fichier voulu dans l’IDE.
Les commandes Maven et les explications figurent dans README.md.
Compilation et tests : mvn clean verify.
