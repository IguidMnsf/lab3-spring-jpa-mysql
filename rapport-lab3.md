# Rapport de réalisation — LAB3

## API REST avec Spring Data JPA, Hibernate et MySQL

**Étudiant(e) :** ____________________

**Date de remise :** ____________________
**Projet :** [lab3-springboot](https://github.com/IguidMnsf/backend-labs/tree/main/lab3-springboot)

## 1. Objectif

Ce laboratoire fait évoluer l'API du LAB2 en ajoutant la persistance relationnelle. Il met en pratique Spring Data JPA, Hibernate, MySQL, les relations entre entités, les transactions et les réponses REST en JSON.

## 2. Réalisation

Le modèle comprend les catégories, les produits, les commandes et les lignes de commande. Les services utilisent des repositories JPA pour accéder aux données. Les réponses sont converties en DTO afin de ne pas exposer directement les entités de la base.

Les routes permettent notamment de créer et consulter les catégories et les produits, de rechercher et paginer les produits, puis de créer et consulter les commandes. Une commande mémorise le prix unitaire au moment de l'achat. La suppression d'un produit utilisé dans une commande est refusée pour préserver les références, avec le statut `409 Conflict`.

Le projet fournit Docker Compose pour démarrer MySQL. Les tests utilisent une base H2 isolée et ne dépendent pas de MySQL.

## 3. Vérifications

La commande `mvn clean verify` a été exécutée avec Java 21. Les **5 tests d'intégration H2 ont réussi**, sans échec ni erreur. Ils couvrent les opérations principales, les relations, la pagination, les commandes, la validation et les erreurs métier.

La configuration Docker Compose a été analysée avec `docker compose config`. Le moteur Docker n'était pas démarré lors de la révision ; par conséquent, la connexion à un serveur MySQL réel n'a pas été vérifiée durant cette session. Les captures de démarrage MySQL et de démonstration Postman doivent être prises lors d'une exécution locale.

## 4. Conclusion

Le laboratoire met en place une API structurée dont les données sont persistées dans MySQL et manipulées avec JPA. Les tests H2 valident le comportement applicatif sans exiger un serveur de base de données pendant la suite automatisée.

## 5. Captures à joindre

Les captures doivent être prises après exécution réelle sur l'ordinateur utilisé pour la démonstration :

1. Docker Desktop ou MySQL local démarré.
2. Terminal PowerShell montrant Spring Boot connecté à la base.
3. Postman affichant une catégorie et un produit créés, avec leurs identifiants.
4. Postman affichant la création d'une commande et son total.
5. Facultatif : refus de suppression d'un produit commandé (`409 Conflict`).
6. Page GitHub du projet ou résultat du workflow GitHub Actions.

**Aucune capture d'écran n'est intégrée à ce rapport.**
