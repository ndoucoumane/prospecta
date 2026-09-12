# Guide de Déploiement CI/CD Prospecta (Serveur 180.149.196.68)

Ce guide détaille la mise en service des environnements **Staging** et **Production** pour l'application **Prospecta Backend** sur le serveur **180.149.196.68** via **Jenkins**.

---

## 1. Vue d'Ensemble de l'Architecture

Les deux environnements cohabitent sur le serveur `180.149.196.68` de manière totalement isolée :

| Composant | Staging | Production |
| :--- | :--- | :--- |
| **Port d'écoute** | `8082` | `8085` *(évite conflit avec Jenkins sur 8080)* |
| **Dossier de travail** | `/opt/prospecta/staging/` | `/opt/prospecta/production/` |
| **Binaire JAR** | `/opt/prospecta/staging/prospecta-backend.jar` | `/opt/prospecta/production/prospecta-backend.jar` |
| **Sauvegardes (Backups)** | `/opt/prospecta/backups/staging/` | `/opt/prospecta/backups/production/` |
| **Fichier d'environnement** | `/opt/prospecta/staging/prospecta-staging.env` | `/opt/prospecta/production/prospecta-prod.env` |
| **Service Systemd** | `prospecta-staging.service` | `prospecta-prod.service` |
| **Profil Spring** | `staging` | `prod` |
| **Actuator Health** | `http://180.149.196.68:8082/actuator/health` | `http://180.149.196.68:8085/actuator/health` |

---

## 2. Étape 1 : Initialisation du Serveur Cible (180.149.196.68)

Connectez-vous en SSH sur le serveur cible `180.149.196.68` :
```bash
ssh root@180.149.196.68
```

Transférez ou clonez le dossier `deploy/` puis exécutez le script d'initialisation :
```bash
sudo bash deploy/setup-server.sh
```

Ce script effectue automatiquement :
- La vérification / installation d'**OpenJDK 21**
- La création de l'utilisateur système `prospecta`
- La création de l'arborescence `/opt/prospecta/` (staging, production, backups)
- L'installation des services systemd `prospecta-staging.service` et `prospecta-prod.service`
- L'activation au démarrage (`systemctl enable`)
- La configuration de `/etc/sudoers.d/prospecta-deploy` pour permettre le redémarrage des services sans mot de passe.

---

## 3. Étape 2 : Configuration des Variables d'Environnement

Éditez les fichiers d'environnement avec vos vraies clés (base de données, Redis, Kafka, Keycloak, etc.) :

### Pour le Staging :
```bash
nano /opt/prospecta/staging/prospecta-staging.env
```

### Pour la Production :
```bash
nano /opt/prospecta/production/prospecta-prod.env
```

Assurez-vous que les permissions soient bien restreintes :
```bash
chmod 600 /opt/prospecta/staging/prospecta-staging.env
chmod 600 /opt/prospecta/production/prospecta-prod.env
chown prospecta:prospecta /opt/prospecta/*/*.env
```

---

## 4. Étape 3 : Configuration de Jenkins

### A. Prérequis Plugins Jenkins
Vérifiez que les plugins suivants sont installés sur votre Jenkins :
- **SSH Agent Plugin** (`ssh-agent`)
- **Pipeline**

### B. Ajout des Identifiants SSH dans Jenkins
1. Rendez-vous dans **Jenkins** > **Tableau de bord** > **Gérer Jenkins** > **Credentials** (Identifiants).
2. Cliquez sur **(global)** > **Add Credentials** :
   - **Kind** : `SSH Username with private key`
   - **ID** : `prospecta-server-ssh` *(correspond à la valeur par défaut dans le Jenkinsfile)*
   - **Description** : `Clé SSH déploiement serveur 180.149.196.68`
   - **Username** : `root` (ou `deploy` / `jenkins`)
   - **Private Key** : Collez le contenu de votre clé privée SSH (ou cochez depuis le fichier `~/.ssh/id_rsa`).
3. Enregistrez.

*(Assurez-vous que la clé publique correspondante est ajoutée dans `~/.ssh/authorized_keys` sur le serveur 180.149.196.68).*

### C. Création du Job Pipeline
1. Créez un nouvel élément > **Pipeline**.
2. Dans la section **Pipeline** :
   - **Definition** : `Pipeline script from SCM`
   - **SCM** : `Git`
   - **Repository URL** : URL de votre dépôt Git Prospecta
   - **Credentials** : Vos accès Git (GitHub / GitLab)
   - **Branch Specifier** : `*/main`
   - **Script Path** : `Jenkinsfile`
3. Sauvegardez.

---

## 5. Fonctionnement du Pipeline Jenkins

Lorsque vous lancez un build (**Build with Parameters**) :
1. **ENVIRONMENT** :
   - Choisissez `staging` pour tester les nouvelles fonctionnalités sur le port `8082`.
   - Choisissez `production` pour déployer sur le port `8085`.
2. **SKIP_TESTS** : Optionnel pour accélérer le build si les tests ont déjà été validés en amont.
3. **Approval Gate** : Si vous choisissez `production`, le pipeline suspend l'exécution et attend une validation manuelle d'un administrateur avant d'appliquer la mise en production.
4. **Sauvegarde automatique** : L'ancien JAR est archivé avec un timestamp Unix dans `/opt/prospecta/backups/<env>/prospecta_<timestamp>.jar`.
5. **Déploiement & Redémarrage** : Le nouveau JAR est transféré et le service `prospecta-<env>` est redémarré.
6. **Health Check** : Le pipeline vérifie jusqu'à 25 fois si `http://180.149.196.68:<port>/actuator/health` retourne le statut `"UP"`.
7. **Rollback automatique** : Si l'application ne démarre pas ou échoue au Health Check, le pipeline restaure immédiatement la dernière version fonctionnelle archivée et redémarre le service.

---

## 6. Commandes Utiles sur le Serveur (180.149.196.68)

### Visualiser le statut des services :
```bash
systemctl status prospecta-staging
systemctl status prospecta-prod
```

### Consulter les logs en temps réel :
```bash
journalctl -u prospecta-staging -f
journalctl -u prospecta-prod -f
```

### Redémarrer manuellement :
```bash
sudo systemctl restart prospecta-staging
sudo systemctl restart prospecta-prod
```

### Tester manuellement les endpoints de santé :
```bash
curl -i http://localhost:8082/actuator/health   # Staging
curl -i http://localhost:8085/actuator/health   # Production
```
