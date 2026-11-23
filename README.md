# LAB3 — Spring Data JPA, Hibernate et MySQL

Ce laboratoire fait évoluer l’API du LAB2 : les produits ne disparaissent plus à l’arrêt du serveur, ils sont enregistrés dans MySQL. Le projet gère aussi les catégories et les commandes, avec les relations entre les entités, des DTO pour les réponses JSON et des tests automatisés.

Le parcours d’une requête est : **Controller → Service → Repository → Hibernate/JPA → MySQL**.

Le rapport est disponible dans [rapport-lab3.md](rapport-lab3.md) et [rapport-lab3.pdf](rapport-lab3.pdf).

## Ce qui est inclus

- Spring Boot 3, Java 21 et Maven.
- Entités `Product`, `Category`, `CustomerOrder` et `OrderItem`.
- Relations `Category 1—N Product`, `CustomerOrder 1—N OrderItem` et `Product 1—N OrderItem`.
- Repositories Spring Data JPA, dont une recherche JPQL par catégorie.
- Services transactionnels et DTO Java `record`, sans exposer les entités JPA en JSON.
- API REST pour les catégories, produits, pagination et commandes.
- Validation des requêtes, erreurs JSON cohérentes et protection des produits utilisés par une commande.
- MySQL 8 avec Docker Compose, ou configuration d’un MySQL installé localement.
- Profil de test H2 : les tests n’ont pas besoin d’un serveur MySQL.
- Collection Postman et scripts SQL d’initialisation/exemple.

## 1. Prérequis

Installe :

- JDK 21 ;
- Maven 3.6 ou supérieur ;
- Postman (facultatif, pour exécuter la collection) ;
- Docker Desktop pour la méthode MySQL la plus simple, **ou** MySQL Server et MySQL Workbench.

Dans PowerShell, vérifie Java et Maven :

```powershell
java -version
mvn -version
```

La sortie de Java doit indiquer la version 21. Si `mvn` n’est pas reconnu, installe Apache Maven ou utilise Maven intégré à IntelliJ IDEA.

Pour installer Java 21 sur Windows avec `winget` :

```powershell
winget install --id EclipseAdoptium.Temurin.21.JDK -e
```

Si `mvn` n'est pas reconnu, installer Apache Maven depuis [la page officielle de téléchargement](https://maven.apache.org/download.cgi), ajouter le dossier `bin` de Maven au `PATH` Windows, puis ouvrir une nouvelle fenêtre PowerShell. Si Chocolatey est déjà installé, utiliser `choco install maven -y`. Vérifier ensuite les versions avec `java -version` et `mvn -version`. Si Java 8 reste sélectionné, configurer le `PATH` pour utiliser le JDK 21.

## 2. Accéder au dépôt local et entrer dans le LAB3

Les commandes supposent un clone sous `$HOME\backend-labs`. Si le dépôt se trouve ailleurs, remplacer ce chemin par son emplacement réel. Pour un clone existant, le mettre à jour puis entrer dans le projet :

```powershell
Set-Location "$HOME\backend-labs"
git pull --ff-only
Set-Location .\lab3-springboot
Get-Location
```

Si le dépôt n'est pas encore cloné :

```powershell
Set-Location $HOME
git clone https://github.com/IguidMnsf/backend-labs.git
Set-Location .\backend-labs\lab3-springboot
```

**Important :** toutes les commandes Maven et Docker Compose ci-dessous sont à lancer depuis `backend-labs\lab3-springboot`.

## 3. Installer MySQL

### Option A — Docker Compose (recommandée)

Ouvre d'abord l'application **Docker Desktop** et attends que son moteur indique qu'il est démarré.

Démarre MySQL :

```powershell
docker compose up -d mysql
```

Vérifie qu’il est prêt :

```powershell
docker compose ps
docker compose logs mysql
```

Le service crée automatiquement la base `lab3_db`. Les paramètres locaux sont déjà réglés dans `application.properties` :

- hôte : `localhost:3306` ;
- base : `lab3_db` ;
- utilisateur : `root` ;
- mot de passe de développement : `lab3pass`.

### Option B — MySQL installé sur l’ordinateur

1. Démarre MySQL Server depuis les services Windows ou MySQL Workbench.
2. Ouvrir MySQL Workbench et se connecter au serveur local.
3. Ouvre `src\main\resources\db\01-create-database.sql` et exécute-le.
4. Définir l'identifiant et le mot de passe MySQL dans le terminal avant de démarrer Spring Boot :

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = Read-Host "Mot de passe MySQL"
```

Si MySQL n’écoute pas sur le port `3306`, définis aussi `DB_URL` :

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3307/lab3_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
```

Ces variables ne sont définies que dans la fenêtre PowerShell courante. Il n’y a pas de vrai mot de passe MySQL stocké dans Git.

## 4. Installer et lancer l’API

Depuis `lab3-springboot` :

```powershell
mvn clean install
mvn spring-boot:run
```

Le serveur écoute sur `http://localhost:8080`. Hibernate crée ou met à jour les tables lors du premier démarrage (`ddl-auto=update`).

Vérifier la réponse de l'API :

```powershell
curl.exe http://localhost:8080/api/products
```

Une liste vide est normale avant la création d'une catégorie et d'un produit.

Pour ouvrir MySQL dans une autre fenêtre PowerShell :

```powershell
docker compose exec mysql mysql -uroot -plab3pass lab3_db
```

Puis vérifier les lignes persistées :

```sql
SELECT * FROM categories;
SELECT * FROM products;
SELECT * FROM customer_orders;
SELECT * FROM order_items;
```

Quitte le client MySQL avec `exit`. Pour arrêter l’API, reviens au terminal du serveur et utilise `Ctrl+C`.

## 5. Créer une catégorie et un produit

Une catégorie doit exister avant la création du produit, car chaque produit appartient à une catégorie.

### Créer la catégorie

```powershell
curl.exe -i -X POST http://localhost:8080/api/categories -H "Content-Type: application/json" -d '{"name":"Électronique"}'
```

La réponse est `201 Created`. Note l’`id` renvoyé : il sera utilisé comme `categoryId`.

### Créer le produit

Remplace `1` par l’identifiant réel de la catégorie :

```powershell
curl.exe -i -X POST http://localhost:8080/api/products -H "Content-Type: application/json" -d '{"name":"Smartphone","price":699.99,"categoryId":1}'
```

La réponse contient le produit, son identifiant et sa catégorie (`categoryId`, `categoryName`). Les entités JPA ne sont pas retournées directement, ce qui évite les boucles JSON entre catégorie et produits.

## 6. Endpoints disponibles

| Méthode | URL | Résultat |
|---|---|---|
| POST | `/api/categories` | Créer une catégorie, `201` |
| GET | `/api/categories` | Lister les catégories |
| GET | `/api/categories/{id}` | Lire une catégorie |
| GET | `/api/categories/{id}/products` | Lister les produits d’une catégorie |
| POST | `/api/products` | Créer un produit lié à une catégorie, `201` |
| GET | `/api/products?page=0&size=10&sort=id&direction=asc` | Lister les produits avec pagination et tri |
| GET | `/api/products/{id}` | Lire un produit |
| GET | `/api/products/search?name=phone` | Rechercher par nom, sans tenir compte de la casse |
| PUT | `/api/products/{id}` | Modifier le nom, le prix ou la catégorie |
| DELETE | `/api/products/{id}` | Supprimer un produit non utilisé dans une commande, `204` |
| POST | `/api/orders` | Créer une commande avec une ou plusieurs lignes, `201` |
| GET | `/api/orders` | Lister les commandes |
| GET | `/api/orders/{id}` | Lire une commande et son total |

Un produit associé à une commande ne peut pas être supprimé : l’API renvoie `409 Conflict` pour préserver l’intégrité des références.

### Créer une commande

Remplace `1` par l’identifiant d’un produit existant :

```powershell
curl.exe -i -X POST http://localhost:8080/api/orders -H "Content-Type: application/json" -d '{"items":[{"productId":1,"quantity":2}]}'
```

La réponse détaille les lignes de commande, le prix unitaire mémorisé et le total. La commande et ses lignes sont créées dans une seule transaction. Le prix de la ligne est copié depuis le produit au moment de la commande afin de préserver le prix d’achat historique.

## 7. Utiliser Postman, pas à pas

Postman envoie les requêtes et affiche les réponses ; il ne démarre ni MySQL ni Spring Boot. Les deux services doivent déjà fonctionner.

1. Installe et ouvre **Postman Desktop**, recommandé pour accéder à `localhost`.
2. Cliquer sur **Import** → **Files**, puis importer `lab3-springboot\postman\LAB3-API.postman_collection.json` depuis le clone local.
3. Dans **Collections**, ouvre **LAB3 - API Produits Spring Data JPA**.
4. Vérifie que le terminal MySQL Docker et le terminal `mvn spring-boot:run` sont encore ouverts.
5. Ouvre **1. Créer une catégorie**, clique **Send**, et vérifie le statut `201 Created`.
6. Continue avec les requêtes dans l'ordre : créer le produit, pagination, lecture, recherche, produits de la catégorie, mise à jour, création d'une commande et lecture de la commande.
7. Pour chaque réponse, le statut HTTP apparaît en haut de la réponse et le JSON dans **Body**. **Test Results** affiche les vérifications automatiques.
8. Termine par les requêtes de données invalides et de suppression d'un produit commandé. Le statut `409 Conflict` de cette dernière est attendu.

L'adresse `http://localhost:8080` désigne l'API exécutée sur l'ordinateur local, et non une adresse GitHub. Les scripts Postman mémorisent les identifiants créés ; utiliser les identifiants retournés plutôt que des valeurs devinées.

Si Postman ne joint pas l'API, vérifier le message de démarrage de Spring Boot et ouvrir `http://localhost:8080/api/products` dans un navigateur. Avec Postman Web, sélectionner l'agent Desktop/local ou utiliser l'application Desktop.

L’identifiant généré dépend des données déjà présentes : les scripts Postman le capturent au lieu de supposer qu’il vaut toujours `1`.

## 8. Exécuter les tests automatisés

Les tests utilisent une base H2 temporaire avec le profil `test`, donc MySQL n’a pas besoin d’être démarré :

```powershell
mvn test
```

Les tests couvrent les relations catégorie-produit, la pagination, la recherche, la mise à jour, les commandes transactionnelles, la validation et les erreurs `404`/`409`.

## 9. Captures d'écran pour le professeur

Utiliser `Win + Shift + S`, sélectionner la zone, puis choisir **Enregistrer sous** dans l'outil Capture d'écran. Enregistrer les images dans un dossier dédié, par exemple `Documents\Captures-Labs\LAB3`.

**Captures en local :**

1. PowerShell avec `docker compose ps`, montrant MySQL démarré.
2. Le terminal `mvn spring-boot:run` après le démarrage réussi de Spring Boot.
3. Postman sur la création du produit : `localhost:8080`, statut `201 Created` et JSON avec catégorie.
4. Postman sur la création/lecture d'une commande, avec ses lignes et le total visibles.
5. Postman sur la requête de suppression protégée, avec `409 Conflict` et son test réussi.
6. Facultatif : PowerShell après `mvn test`, avec `BUILD SUCCESS`.

**Captures du projet publié sur GitHub :**

1. Ouvre [les fichiers LAB3 sur GitHub](https://github.com/IguidMnsf/backend-labs/tree/main/lab3-springboot) et capture le dossier.
2. Capture le README affiché par GitHub.
3. Dans **Actions**, ouvre un run récent et capture le job `lab3-spring-jpa` terminé avec succès.
4. Facultatif : affiche la collection Postman ou `pom.xml` sur GitHub.

GitHub héberge le code et exécute les tests automatiques. L'API du LAB3 n'est pas déployée sur Internet : elle s'exécute localement avec MySQL ou Docker.

## 10. Données d’exemple SQL

Hibernate crée les tables à partir des entités. Pour ajouter le jeu d'exemple du sujet, après le premier démarrage du serveur, ouvrir un client MySQL et exécuter :

```powershell
Get-Content -Raw src\main\resources\db\02-sample-data.sql | docker compose exec -T mysql mysql -uroot -plab3pass lab3_db
```

Le fichier `src\main\resources\db\02-sample-data.sql` peut également être ouvert et exécuté dans MySQL Workbench.

Les données d’exemple sont facultatives : la collection Postman crée elle-même ses données de test.

## 11. Comprendre les relations et le JSON

- `Product` possède une relation `ManyToOne` vers `Category`, chargée en `LAZY`.
- `Category` expose une collection `OneToMany` en lecture interne, sans cascade de suppression dangereuse.
- `CustomerOrder` possède plusieurs `OrderItem` ; la suppression des lignes suit le cycle de vie de la commande.
- Chaque `OrderItem` fait référence à un produit et mémorise quantité et prix unitaire.
- Les contrôleurs retournent des DTO, pas les entités JPA : cela évite les cycles de sérialisation et le chargement accidentel de relations.
- Les modifications métier sont encadrées par `@Transactional`.

## Dépannage pas à pas

**`docker` n'est pas reconnu ou affiche une erreur de connexion au moteur**

1. Installer et ouvrir Docker Desktop.
2. Attendre que Docker Desktop indique que le moteur est démarré.
3. Depuis `lab3-springboot`, vérifier `docker version` puis `docker compose ps`.
4. Si l'erreur persiste, redémarrer Docker Desktop puis réessayer `docker compose up -d mysql`.

**MySQL ne démarre pas, reste `unhealthy` ou affiche `port is already allocated`**

1. Lire l'état et les journaux :

```powershell
docker compose ps
docker compose logs --tail 50 mysql
```

2. Si le port `3306` est déjà utilisé, un MySQL Windows ou le LAB4 peut en être la cause. Arrêter le serveur MySQL concurrent. Pour arrêter le LAB4, ouvrir une autre fenêtre PowerShell, accéder à `"$HOME\backend-labs\lab4-spring-security"` et exécuter `docker compose down`.
3. Si le port doit rester occupé, modifier temporairement dans `docker-compose.yml` le mappage en `3307:3306`, puis dans la même fenêtre que Spring Boot définir `$env:DB_URL = "jdbc:mysql://localhost:3307/lab3_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"`.
4. Relancer `docker compose up -d mysql` et attendre l'état `healthy` avant de lancer l'API.

**`Communications link failure` ou connexion refusée à MySQL**

1. Vérifier `docker compose ps` et attendre que `lab3-mysql` soit `healthy`.
2. Contrôler que Spring Boot utilise `localhost:3306` (ou le port local choisi à l'étape précédente) et la base `lab3_db`.
3. Examiner `docker compose logs --tail 50 mysql`.
4. Démarrer l'application depuis le dossier du LAB3 et non depuis un autre laboratoire.

**`Access denied for user` ou `Unknown database`**

Avec Docker, les valeurs par défaut de ce laboratoire sont `root`, `lab3pass` et `lab3_db`. Avec un MySQL local, vérifier que les variables `$env:DB_USERNAME`, `$env:DB_PASSWORD` et éventuellement `$env:DB_URL` sont définies dans la même fenêtre PowerShell que `mvn spring-boot:run`. Vérifier également que le script `src\main\resources\db\01-create-database.sql` a été exécuté pour un MySQL installé localement.

Si un mot de passe MySQL de Docker a été changé après la première création du volume, le conteneur existant conserve l'ancien compte. Utiliser les identifiants initiaux ou réinitialiser le volume comme indiqué ci-dessous.

**Le port `8080` est déjà utilisé**

Arrêter l'autre serveur avec `Ctrl+C`, ou choisir le port `8081` dans la même fenêtre PowerShell :

```powershell
$env:SERVER_PORT = "8081"
mvn spring-boot:run
```

Dans Postman, modifier `baseUrl` en `http://localhost:8081`.

**`mvn` échoue avant le démarrage ou depuis un mauvais répertoire**

Vérifier que `Get-Location` se termine par `backend-labs\lab3-springboot`, puis que `Test-Path .\pom.xml` affiche `True`. Les commandes Compose et Maven doivent être exécutées depuis ce dossier. Vérifier également `java -version` (21) et `mvn -version`.

**Postman reçoit `404`, `400` ou `409`**

1. Pour créer un produit, créer d'abord une catégorie et utiliser le `categoryId` réellement retourné.
2. Rejouer les requêtes de création dans l'ordre et utiliser les identifiants capturés par la collection.
3. `400` indique généralement un champ manquant ou invalide ; vérifier le JSON et le type de contenu `application/json`.
4. `409 Conflict` lors de la suppression d'un produit utilisé dans une commande est attendu : supprimer d'abord la commande ou garder ce résultat comme démonstration de la règle métier.

**Les données semblent avoir disparu**

Vérifier que l'application pointe vers la même base `lab3_db` et le même port. Avec Docker, `docker compose down` arrête les conteneurs mais conserve le volume ; ne pas confondre avec `docker compose down -v`. Les tests `mvn test` utilisent H2 temporaire et n'écrivent pas dans la base MySQL de démonstration.

**Recommencer avec une base Docker vide**

Cette opération supprime définitivement toutes les données du LAB3 dans le volume local. Ne l'utiliser que si une remise à zéro est souhaitée :

```powershell
docker compose down -v
docker compose up -d mysql
docker compose ps
```

Attendre l'état `healthy`, puis redémarrer Spring Boot. Pour éviter de supprimer des données à l'avenir, utiliser `docker compose down` sans `-v`.

## Résultat attendu

**Résultat attendu :** une API Spring Boot qui conserve les catégories, produits et commandes dans MySQL, un modèle relationnel cohérent, des réponses JSON sans cycle et une collection Postman pour tester le parcours complet.
