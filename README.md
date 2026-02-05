# Product Service - Pipeline CI DevOps (PFE)

## 📋 Table des matières
1. [Introduction](#1-introduction)
2. [Périmètre](#2-périmètre)
3. [Statistiques du projet](#3-statistiques-du-projet)
4. [Outils utilisés et rôle dans la chaîne CI](#4-outils-utilisés-et-rôle-dans-la-chaîne-ci)
5. [Workflow du pipeline backend](#5-workflow-du-pipeline-backend-jusquau-push-harbor)
6. [Détail et justification de chaque stage](#6-détail-et-justification-de-chaque-stage)
7. [Politique de gates](#7-politique-de-gates-critères-de-blocage)
8. [Collecte des logs et auditabilité](#8-collecte-des-logs-et-auditabilité-jenkins--elk)
9. [Configuration du Jenkinsfile](#9-configuration-du-jenkinsfile)
10. [Conclusion](#10-conclusion)

---

## 1. Introduction

Dans le cadre d'une mission DevOps (PFE), l'objectif est de mettre en place un pipeline CI permettant de produire une image conteneur **fiable**, **traçable** et **sécurisée**, avant de la pousser vers un registry privé (Harbor).

L'approche décrite est basée sur le principe **"fail fast"** et sur l'intégration de contrôles de qualité et de sécurité (**shift-left**) afin d'empêcher la publication d'une image non conforme.

> **Message clé** : l'objectif n'est pas de promettre un risque zéro, mais de fournir un **niveau de confiance mesurable et auditable** avant diffusion d'un artefact (image).

---

## 2. Périmètre

### ✅ Inclus dans ce document
Le pipeline présenté s'arrête à l'étape :
- **Push de l'image conteneur vers un registry privé (Harbor)**

### ❌ Hors périmètre
- Déploiement en staging/production
- DAST (ex. OWASP ZAP) nécessitant une application exposée publiquement
- Tests E2E UI complets sur environnement déployé

---

## 3. Statistiques du projet

### 📊 Vue d'ensemble

| Métrique | Valeur |
|----------|--------|
| **Langage** | Java 17 (Spring Boot 3.2.2) |
| **Base de données** | H2 (in-memory) |
| **Tests totaux** | **120 tests** |
| **Couverture de code** | Analysée par JaCoCo (28 classes) |

### 🧪 Répartition des tests

#### Tests Unitaires + Intégration (JUnit 5)

| Catégorie | Fichier de test | Nombre de tests | Statut |
|-----------|-----------------|-----------------|--------|
| **Controller** | `ProductControllerTest` | 19 | ✅ PASS |
| **Controller** | `OrderControllerTest` | 10 | ✅ PASS |
| **Service** | `ProductServiceTest` | 23 | ✅ PASS |
| **Service** | `OrderServiceTest` | 20 | ✅ PASS |
| **Repository** | `ProductRepositoryTest` | 11 | ✅ PASS |
| **Intégration** | `ProductIntegrationTest` | 6 | ✅ PASS |
| **Context** | `ProductServiceApplicationTests` | 1 | ✅ PASS |
| | **TOTAL JUnit** | **90 tests** | ✅ |

#### Tests E2E / API (Robot Framework)

| Catégorie | Tag | Nombre de tests | Description |
|-----------|-----|-----------------|-------------|
| **Health Checks** | `smoke`, `health` | 4 | Actuator, Kubernetes probes |
| **CRUD Products** | `crud`, `product` | 5 | Create, Read, Update, Delete |
| **CRUD Orders** | `crud`, `order` | 3 | Création et récupération commandes |
| **Recherche/Filtres** | `search` | 4 | Catégorie, keyword, prix, stock |
| **Gestion Stock** | `stock` | 3 | Add, Remove, Check |
| **Gestion Erreurs** | `error` | 2 | 404, 400 validation |
| **Workflows E2E** | `e2e`, `regression` | 9 | Scénarios complets bout-en-bout |
| | **TOTAL Robot** | **30 tests** | ✅ |

#### Détail des tests E2E (Non-Régression)

| Test E2E | Description | Criticité |
|----------|-------------|-----------|
| Complete Order Workflow | Créer → Commander → Confirmer → Expédier → Livrer | 🔴 CRITICAL |
| Order Cancellation Workflow | Commander → Annuler + restauration stock | 🟡 HIGH |
| Insufficient Stock Order | Commander > stock → Échec 400 | 🟡 HIGH |
| Order Status Transition | Transitions invalides → Échec 400 | 🟡 HIGH |
| Multiple Products Order | Multi-produits + calcul total | 🟢 MEDIUM |
| Orders By Customer Email | Recherche par email client | 🟢 MEDIUM |
| Orders By Status | Filtre par statut | 🟢 MEDIUM |
| Product Activation/Deactivation | Toggle actif/inactif | 🟢 MEDIUM |
| Order Inactive Product | Commander produit inactif → Échec | 🟡 HIGH |

#### 🔄 Workflows Multi-API (Robot Framework)

Robot Framework teste des **workflows complets avec plusieurs appels API enchaînés** :

```robot
*** Test Cases ***
Complete Order Workflow From Creation To Delivery
    # Étape 1: Créer un produit avec stock initial
    ${product}=    POST    /api/v1/products    {...}
    
    # Étape 2: Créer une commande pour ce produit
    ${order}=    POST    /api/v1/orders    {...}
    
    # Étape 3: Confirmer la commande
    PUT    /api/v1/orders/${order.id}/confirm
    
    # Étape 4: Expédier la commande
    PUT    /api/v1/orders/${order.id}/ship
    
    # Étape 5: Marquer comme livrée
    PUT    /api/v1/orders/${order.id}/deliver
    
    # Étape 6: Vérifier que le stock a diminué
    ${updated}=    GET    /api/v1/products/${product.id}
    Should Be Equal    ${updated.stockQuantity}    ${expected}
```

**Avantages des workflows multi-API :**
- ✅ Teste la logique métier complète (pas juste les endpoints isolés)
- ✅ Vérifie les effets de bord (stock, statuts, dates)
- ✅ Détecte les régressions dans les enchaînements
- ✅ Simule le comportement réel d'un client API

### 📈 Pyramide des tests

```
                    🔺 E2E (Robot Framework)
                   /    30 tests API (black-box)
                  ────────────────────────────
                 🔷 INTÉGRATION (Spring Boot Test)
                /     7 tests avec contexte complet
               ─────────────────────────────────────
              🟢 UNITAIRES (JUnit + Mockito)
             /     83 tests isolés avec mocks
            ───────────────────────────────────────────
```

### � Détail des Tests d'Intégration

Les tests d'intégration vérifient le **flux complet** : Controller → Service → Repository → Base de données H2

#### ProductIntegrationTest (6 tests) - Tests API End-to-End

| Test | Ce qu'il vérifie |
|------|------------------|
| `shouldCreateAndRetrieveProduct` | Créer un produit via POST, le récupérer via GET, vérifier persistance en BDD |
| `shouldUpdateProduct` | Modifier un produit via PUT et vérifier les changements en BDD |
| `shouldManageStock` | Ajouter/Retirer du stock via endpoints et vérifier quantités en BDD |
| `shouldSearchAndFilterProducts` | Recherche et filtrage des produits par critères |
| `shouldDeleteProduct` | Suppression d'un produit et vérification en BDD |
| `shouldHandleNotFoundProduct` | Gestion erreur 404 pour produit inexistant |

#### ProductRepositoryTest (11 tests) - Tests de la couche JPA

| Test | Ce qu'il vérifie |
|------|------------------|
| `shouldFindProductBySku` | Recherche par SKU unique fonctionne |
| `shouldReturnEmptyWhenSkuNotFound` | Retourne vide si SKU inexistant |
| `shouldFindProductsByCategory` | Filtrer les produits par catégorie |
| `shouldFindOnlyActiveProducts` | Récupérer que les produits actifs |
| `shouldFindOnlyInactiveProducts` | Récupérer que les produits désactivés |
| `shouldFindLowStockProducts` | Trouver les produits en stock faible (< seuil) |
| `shouldFindProductsByPriceRange` | Filtrer par fourchette de prix |
| `shouldSearchProductsByKeywordInName` | Recherche textuelle dans le nom |
| `shouldCheckSkuExists` | Vérifier l'existence d'un SKU |
| `shouldCountByCategory` | Compter les produits par catégorie |
| `shouldFindOutOfStockProducts` | Trouver les produits en rupture de stock |

### �📁 Structure des classes analysées (JaCoCo)

| Package | Classes | Description |
|---------|---------|-------------|
| `controller` | 3 | ProductController, OrderController, HealthController |
| `service.impl` | 2 | ProductServiceImpl, OrderServiceImpl |
| `repository` | 3 | ProductRepository, OrderRepository, OrderItemRepository |
| `entity` | 4 | Product, Order, OrderItem, OrderStatus |
| `dto` | 3 | ProductDTO, OrderDTO, OrderItemDTO |
| `exception` | 5 | GlobalExceptionHandler, exceptions métier |
| `mapper` | 2 | ProductMapper, OrderMapper |
| **TOTAL** | **28 classes** | Couverture analysée par JaCoCo |

---

## 4. Outils utilisés et rôle dans la chaîne CI

| Outil | Version | Rôle / Utilité |
|-------|---------|----------------|
| **Maven** | 3.9.6 | Build Java : compilation, tests unitaires, packaging. Builds reproductibles et intégration plugins qualité/sécurité. |
| **JUnit 5** | 5.10.x | Framework de tests : unitaires, intégration, paramétrisés. |
| **JaCoCo** | 0.8.11 | Métriques de couverture de code. Seuil minimum : **70%**. |
| **SonarQube** | - | Analyse SAST + qualité : bugs, vulnérabilités statiques, duplications, code smells. Quality Gate bloquante. |
| **OWASP Dependency-Check** | 9.0.7 | SCA dépendances : détecte CVE dans librairies Maven (directes et transitives). |
| **Jib** | 3.4.0 | Construction d'image container sans Dockerfile, couches optimisées, build rapide. |
| **Trivy** | latest | Scan vulnérabilités image : OS/packages + artefacts applicatifs. Gate sécurité. |
| **Docker** | - | Exécution image en CI pour valider comportement runtime réel. |
| **Robot Framework** | 6.x | Tests de régression API (black-box). Rapports HTML + JUnit. |
| **Harbor** | - | Registry privé : stockage images versionnées (tags + digest). |

---

## 5. Workflow du pipeline backend (jusqu'au push Harbor)

### 5.1 Principes structurants

- **Fail fast** : arrêter tôt si une étape critique échoue
- **Shift-left** : qualité et sécurité avant publication
- **Traçabilité** : tag immuable basé sur `SHORT_SHA`
- **Confiance runtime** : démarrer le conteneur et valider l'API (health + régression)

### 5.2 Résumé des stages et livrables

| # | Stage | Objectif | Livrables |
|---|-------|----------|-----------|
| 1 | `checkout-init` | Récupérer code + calculer variables | Workspace + `SHORT_SHA`, `IMAGE_REF` |
| 2 | `build-compile` | Vérifier compilation (fail-fast) | Classes compilées (`target/`) |
| 3 | `unit-tests` | Non-régression rapide (**90 tests**) | Rapports JUnit + JaCoCo |
| 4 | `sonarqube-sast-quality` | Analyse qualité/sécurité statique | Dashboard SonarQube |
| 5 | `quality-gate` | Bloquer si non conforme | PASS/FAIL gate |
| 6 | `sca-dependencies` | CVE dépendances (SCA) | Rapport Dependency-Check |
| 7 | `package` | Générer le jar exécutable | `target/*.jar` |
| 8 | `image-build-jib-local` | Construire l'image (local) | Image Docker taggée SHA |
| 9 | `trivy-image-scan` | Bloquer image vulnérable | Logs + rapport Trivy |
| 10 | `container-smoke-runtime` | Vérifier démarrage + readiness | Logs conteneur + health OK |
| 11 | `robot-api-regression` | Régression API (**30 tests**) | Rapports Robot (HTML/XML) |
| 12 | `push-harbor` | Publier l'image validée | Image Harbor + digest |

---

## 6. Détail et justification de chaque stage

### Stage 1 – checkout-init
**Utilité** : Assurer reproductibilité et traçabilité. Calcul de `SHORT_SHA` (tag immuable).

> Un tag basé sur commit évite `latest` et permet audit/rollback.

### Stage 2 – build-compile
**Utilité** : Détecter immédiatement les erreurs de compilation.

**Gate** : Compilation KO ⇒ ARRÊT

### Stage 3 – unit-tests
**Utilité** : Non-régression rapide et protection contre les erreurs introduites.

| Métrique | Valeur |
|----------|--------|
| Tests exécutés | **90** |
| Tests réussis | **90** |
| Tests échoués | **0** |
| Tests ignorés | **0** |
| Durée | ~39 secondes |
| Classes analysées | **28** |

**Livrables** :
- `target/surefire-reports/*.xml` (28 fichiers)
- `target/site/jacoco/index.html`
- `target/jacoco.exec`

**Gate** : Test KO ⇒ ARRÊT

### Stage 4 – sonarqube-sast-quality
**Utilité** : Analyser qualité et sécurité statique (sans exécution).

**Sortie** : Métriques SonarQube (bugs, vulnérabilités, duplications)

### Stage 5 – quality-gate
**Utilité** : Imposer politique de conformité et stopper si non respectée.

**Gate** : FAIL ⇒ ARRÊT

### Stage 6 – sca-dependencies
**Utilité** : Détecter vulnérabilités des dépendances (supply chain).

**Sortie** : Rapport Dependency-Check

**Gate** : CVSS ≥ 9 ⇒ ARRÊT (configurable)

### Stage 7 – package
**Utilité** : Produire l'artefact exécutable.

**Sortie** : `target/product-service-1.0.0-SNAPSHOT.jar`

### Stage 8 – image-build-jib-local
**Utilité** : Construire image container localement pour contrôle avant publication.

**Image** : `product-service:latest`, `product-service:1.0.0-SNAPSHOT`

**Base image** : `eclipse-temurin:17-jre-alpine`

### Stage 9 – trivy-image-scan
**Utilité** : Détecter vulnérabilités dans l'image avant diffusion.

**Gate** : Vulnérabilités CRITICAL ⇒ ARRÊT

### Stage 10 – container-smoke-runtime
**Utilité** : Prouver que l'image démarre et expose un service UP.

**Méthode** : 
```bash
docker run -d product-service
curl http://localhost:8080/actuator/health
```

**Gate** : Health KO ou timeout ⇒ ARRÊT

### Stage 11 – robot-api-regression
**Utilité** : Vérifier logique métier exposée par l'API (black-box).

| Métrique | Valeur |
|----------|--------|
| Tests E2E | **30** |
| Tests Health | 4 |
| Tests CRUD | 8 |
| Tests Recherche | 4 |
| Tests Stock | 3 |
| Tests E2E Workflow | 9 |
| Tests Erreurs | 2 |

**Livrables** :
- `robot-tests/report.html`
- `robot-tests/log.html`
- `robot-tests/output.xml`
- `robot-tests/xunit.xml`

**Gate** : Test KO ⇒ ARRÊT

### Stage 12 – push-harbor
**Utilité** : Publier image uniquement si tous les contrôles passés.

**Sortie** : Image dans Harbor (`harbor.local/devops/product-service:<SHA>`)

---

## 7. Politique de gates (critères de blocage)

| Gate | Condition de blocage | Impact |
|------|---------------------|--------|
| Compilation | Échec | 🛑 ARRÊT |
| Unit tests | 1+ test KO | 🛑 ARRÊT |
| Quality Gate SonarQube | FAIL | 🛑 ARRÊT |
| Couverture JaCoCo | < 70% | 🛑 ARRÊT |
| SCA Dependency-Check | CVSS ≥ 9 | 🛑 ARRÊT |
| Trivy | Vulnérabilité CRITICAL | 🛑 ARRÊT |
| Smoke runtime | Health KO | 🛑 ARRÊT |
| Robot Framework | 1+ test KO | 🛑 ARRÊT |
| Push registry | Push KO | 🛑 ARRÊT |

---

## 8. Collecte des logs et auditabilité (Jenkins → ELK)

### 8.1 Ce qui est collectable

| Type | Source | Format |
|------|--------|--------|
| Logs pipeline | Jenkins console | Texte |
| Logs applicatifs | `docker logs` | Texte |
| Rapports JUnit | Surefire | XML (28 fichiers) |
| Rapport couverture | JaCoCo | HTML + XML |
| Rapport dépendances | Dependency-Check | HTML + XML |
| Rapport scan image | Trivy | JSON |
| Rapports API | Robot Framework | HTML + XML |

### 8.2 Approche ELK recommandée

1. **Filebeat/Fluent Bit** : collecte logs (controller/agents) → Logstash/Elasticsearch
2. **Kibana** : dashboards personnalisés (échecs par stage, tendances, erreurs fréquentes)

---

## 9. Configuration du Jenkinsfile

### 📋 Variables d'environnement

```groovy
environment {
    // Registry Harbor
    HARBOR_REGISTRY = 'harbor.local'
    HARBOR_PROJECT = 'devops'
    IMAGE_NAME = 'product-service'
    HARBOR_CREDENTIALS = 'harbor-credentials'
    
    // SonarQube
    SONAR_HOST_URL = 'http://sonarqube:9000'
    SONAR_PROJECT_KEY = 'product-service'
    
    // Seuils de sécurité
    TRIVY_SEVERITY = 'CRITICAL,HIGH'
    DEPENDENCY_CHECK_FAIL_SCORE = '9'
}
```

### 🔧 Prérequis Jenkins

| Plugin | Utilité |
|--------|--------|
| **Pipeline** | Exécution du Jenkinsfile |
| **JaCoCo** | Publication rapports couverture |
| **JUnit** | Publication résultats tests |
| **SonarQube Scanner** | Intégration SonarQube |
| **Dependency-Check** | Publication rapports OWASP |
| **Robot Framework** | Publication rapports Robot |
| **Docker Pipeline** | Manipulation images Docker |
| **Credentials Binding** | Gestion secrets Harbor |

### 🏗️ Architecture des 12 Stages

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         PIPELINE CI - 12 STAGES                          │
├─────────────────────────────────────────────────────────────────────────┤
│  1. checkout-init          → Clone + calcul SHORT_SHA                   │
│  2. build-compile          → Compilation Maven (fail fast)              │
│  3. unit-tests             → 90 tests JUnit + JaCoCo                    │
│  4. sonarqube-sast-quality → Analyse statique SAST                      │
│  5. quality-gate           → Validation SonarQube (bloquant)            │
│  6. sca-dependencies       → OWASP Dependency-Check                     │
│  7. package                → Création JAR                               │
│  8. image-build-jib-local  → Build Docker avec Jib                      │
│  9. trivy-image-scan       → Scan vulnérabilités image                  │
│ 10. container-smoke-runtime→ Test conteneur + /actuator/health          │
│ 11. robot-api-regression   → 30 tests E2E Robot Framework               │
│ 12. push-harbor            → Push vers Harbor registry                  │
└─────────────────────────────────────────────────────────────────────────┘
```

### 🐳 Exécution Robot Framework en Docker

```groovy
stage('11-robot-api-regression') {
    steps {
        sh '''
            docker run --rm \
                --network host \
                -v $(pwd)/robot-tests:/tests \
                -v $(pwd)/robot-reports:/reports \
                robotframework/rfdocker:latest \
                robot --outputdir /reports /tests/api_tests.robot
        '''
    }
}
```

### 📊 Artifacts générés par le pipeline

| Stage | Artifacts |
|-------|----------|
| unit-tests | `target/surefire-reports/**/*`, `target/site/jacoco/**/*` |
| sca-dependencies | `target/dependency-check-report.*` |
| package | `target/*.jar` |
| robot-api-regression | `robot-reports/**/*` |

---

## 10. Conclusion

Le pipeline décrit fournit une **chaîne de contrôle complète** jusqu'au push dans un registry privé.

### ✅ Bénéfices

| Aspect | Mécanisme | Résultat |
|--------|-----------|----------|
| **Fiabilité** | 90 tests unitaires + smoke runtime | Non-régression garantie |
| **Qualité** | SonarQube + Quality Gate | Code conforme aux standards |
| **Sécurité supply chain** | Dependency-Check + Trivy | Vulnérabilités détectées |
| **Logique métier** | 30 tests Robot Framework | API validée en black-box |
| **Traçabilité** | Tags SHA + Harbor | Images auditables |

### 📊 Résumé des statistiques

```
┌────────────────────────────────────────────────────────────┐
│                    STATISTIQUES PROJET                      │
├────────────────────────────────────────────────────────────┤
│  📦 Classes Java analysées          : 28                   │
│  🧪 Tests unitaires/intégration     : 90  (100% PASS)      │
│  🤖 Tests E2E Robot Framework       : 30                   │
│  📈 Total tests                     : 120                  │
│  📁 Rapports Surefire générés       : 28 fichiers XML      │
│  🎯 Seuil couverture JaCoCo         : 70%                  │
│  🐳 Image Docker                    : product-service      │
│  🏷️ Tags                            : latest, 1.0.0-SNAPSHOT│
└────────────────────────────────────────────────────────────┘
```

### 🚀 Prochaine étape recommandée

Compléter avec une **pipeline CD** (staging) incluant :
- Tests E2E/DAST (OWASP ZAP)
- Stratégie de promotion d'images (quarantaine/approbation)
- Déploiement Kubernetes

---

## 📁 Arborescence du projet

```
product-service/
├── src/
│   ├── main/
│   │   ├── java/com/devops/pfe/
│   │   │   ├── controller/        # 3 contrôleurs REST
│   │   │   ├── service/           # 2 services métier
│   │   │   ├── repository/        # 3 repositories JPA
│   │   │   ├── entity/            # 4 entités
│   │   │   ├── dto/               # 3 DTOs
│   │   │   ├── exception/         # 5 classes exception
│   │   │   └── mapper/            # 2 mappers
│   │   └── resources/
│   │       ├── application.yml
│   │       └── data.sql
│   └── test/
│       └── java/com/devops/pfe/   # 90 tests JUnit
├── robot-tests/
│   └── api_tests.robot            # 30 tests E2E
├── target/
│   ├── surefire-reports/          # 28 rapports XML
│   └── site/jacoco/               # Rapport couverture HTML
├── pom.xml                        # Configuration Maven
├── Jenkinsfile                    # Pipeline CI
└── README.md                      # Ce document
```

---

## 🛠️ Commandes utiles

### Build et Tests
```bash
# Compiler
./mvnw compile

# Tests unitaires + intégration
./mvnw test

# Package (JAR)
./mvnw package -DskipTests

# Build image Docker avec Jib
./mvnw jib:dockerBuild -Djib.to.image=product-service:latest

# Lancer l'application
./mvnw spring-boot:run

# Tests Robot Framework (nécessite Python + app démarrée)
robot robot-tests/api_tests.robot
```

### Rapports
```bash
# Ouvrir rapport JaCoCo
start target/site/jacoco/index.html

# Lister rapports Surefire
ls target/surefire-reports/*.xml
```

---

**Auteur** : Pipeline DevOps PFE  
**Version** : 1.0.0-SNAPSHOT  
**Date** : Février 2026
