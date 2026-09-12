# Guide de Déploiement CI/CD Prospecta (Docker & Jenkins sur 180.149.196.68)

Ce guide détaille la mise en service et le pipeline CI/CD automatisé pour l'application **Prospecta Backend** sur le serveur **180.149.196.68** via **Docker**, **Docker Compose** et **Jenkins**.

---

## 1. Vue d'Ensemble de l'Architecture Conteneurisée

Les conteneurs de l'application cohabitent sur le serveur `180.149.196.68` et sont interconnectés via le réseau Docker **`infrastructure-network`** :

| Composant | Staging | Production | Rôle & Remarques |
| :--- | :--- | :--- | :--- |
| **Port d'écoute VPS** | `127.0.0.1:8086` | `127.0.0.1:8085` | Restreint au localhost (derrière Reverse Proxy Nginx/Caddy) |
| **Nom du conteneur** | `prospecta-backend-staging` | `prospecta-backend-prod` | Conteneur Docker Java 21 Alpine |
| **Réseau Docker** | `infrastructure-network` | `infrastructure-network` | Réseau bridge partagé avec PostgreSQL, Redis, Kafka |
| **Dossier de travail** | `/opt/prospecta/staging/` | `/opt/prospecta/production/` | Contient `docker-compose.yml` et `.env` |
| **Fichier d'environnement** | `/opt/prospecta/staging/prospecta-staging.env` | `/opt/prospecta/production/prospecta-prod.env` | Secrets injectés au démarrage du conteneur |
| **Profil Spring** | `staging` | `prod` | `SPRING_PROFILES_ACTIVE` |
| **Endpoint Santé (Actuator)**| `http://127.0.0.1:8086/actuator/health` | `http://127.0.0.1:8085/actuator/health` | Vérifié en local par Jenkins via SSH |

### Répartition des ports sur le serveur :
- **8085** : Prospecta Backend Production (`127.0.0.1:8085`)
- **8086** : Prospecta Backend Staging (`127.0.0.1:8086`)
- **8082** : Keycloak Auth Server (`127.0.0.1:8082`)
- **8080 / 8083** : Jenkins CI/CD

---

## 2. Étape 1 : Initialisation du Serveur Cible (180.149.196.68)

Connectez-vous en SSH sur le serveur cible `180.149.196.68` :
```bash
ssh root@180.149.196.68
```

Transférez le dossier `deploy/` ou exécutez le script d'initialisation :
```bash
sudo bash deploy/setup-server.sh
```

Ce script effectue automatiquement :
- L'installation et la configuration de **Docker Engine** et du plugin **docker compose**
- La création du réseau Docker externe **`infrastructure-network`**
- La création de l'arborescence `/opt/prospecta/production` et `/opt/prospecta/staging`
- L'installation des fichiers `docker-compose.yml`
- La création de l'utilisateur `deploy` avec les permissions Docker requises

---

## 3. Étape 2 : Configuration des Fichiers d'Environnement

Éditez les fichiers d'environnement avec vos mots de passe et clés API réelles :

### Pour la Production :
```bash
sudo nano /opt/prospecta/production/prospecta-prod.env
```
*(Configurez `DATABASE_PASSWORD`, `OPENAI_API_KEY`, etc.)*

### Pour le Staging :
```bash
sudo nano /opt/prospecta/staging/prospecta-staging.env
```

Sécurisez les permissions :
```bash
sudo chmod 600 /opt/prospecta/*/*.env
```

---

## 4. Étape 3 : Configuration de Jenkins

### A. Prérequis Plugins Jenkins
- **SSH Agent Plugin** (`ssh-agent`)
- **Pipeline**

### B. Ajout du Secret SSH dans Jenkins
1. Rendez-vous dans **Jenkins** > **Tableau de bord** > **Gérer Jenkins** > **Credentials**.
2. Cliquez sur **(global)** > **Add Credentials** :
   - **Kind** : `SSH Username with private key`
   - **ID** : `prospecta-server-ssh`
   - **Username** : `root` (ou `deploy`)
   - **Private Key** : Clé privée SSH autorisée sur `180.149.196.68`

---

## 5. Fonctionnement du Pipeline Jenkins (`Jenkinsfile`)

Le pipeline exécute le cycle complet en conteneur Docker :
1. **Checkout** : Récupère la dernière version du code source Git.
2. **Init & Configuration** :
   - Calcule le port (`8085` pour prod, `8086` pour staging).
   - Configure les tags d'image Docker (`prod-<BUILD_NUMBER>`, `staging-<BUILD_NUMBER>`).
3. **Inspect Running Container** : Détecte l'image active pour permettre un rollback ciblé.
4. **Build & Test Maven** : Compile le JAR avec Java 21 et exécute les tests unitaires.
5. **Build Docker Image** : Construit l'image Docker locale (`Dockerfile`).
6. **Approval Gate (Production)** : Interruption pour validation humaine avant la mise en production.
7. **Deploy to Server** :
   - Transfère l'image Docker vers le VPS (via Registry ou streaming SSH compressé `docker save | gzip | docker load`).
   - Copie `docker-compose.yml`.
   - Lance le conteneur : `IMAGE_TAG=<tag> docker compose up -d --remove-orphans`.
8. **Health Check Local** :
   - Exécute le curl sur `http://127.0.0.1:<PORT>/actuator/health` **directement depuis le VPS**.
   - En cas d'échec : affiche les 100 dernières lignes de logs (`docker logs`) et restaure l'ancienne image.
9. **Nettoyage automatique** : Prune des anciennes images orphelines et nettoyage du workspace.

---

## 6. Commandes Utiles sur le Serveur (180.149.196.68)

### Visualiser l'état des conteneurs :
```bash
docker ps
cd /opt/prospecta/production && docker compose ps
cd /opt/prospecta/staging && docker compose ps
```

### Consulter les logs en direct :
```bash
docker logs -f prospecta-backend-prod
docker logs -f prospecta-backend-staging
```

### Redémarrer manuellement :
```bash
cd /opt/prospecta/production && docker compose restart
```

### Tester l'état de santé localement :
```bash
curl -i http://127.0.0.1:8085/actuator/health   # Production
curl -i http://127.0.0.1:8086/actuator/health   # Staging
```
