#!/usr/bin/env bash
# ==============================================================================
# PROSPECTA - Script d'Initialisation du Serveur Cible (180.149.196.68)
# Ce script prépare les répertoires, l'utilisateur système, les services systemd
# et les permissions nécessaires pour Staging et Production.
#
# Exécution : sudo bash deploy/setup-server.sh
# ==============================================================================

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=== [1/6] Vérification des privilèges root ===${NC}"
if [[ $EUID -ne 0 ]]; then
   echo -e "${RED}Ce script doit être exécuté avec les privilèges root (sudo).${NC}"
   exit 1
fi

echo -e "${BLUE}=== [2/6] Vérification de Java 21 ===${NC}"
if command -v java >/dev/null 2>&1; then
    JAVA_VER=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')
    echo -e "${GREEN}Java détecté : ${JAVA_VER}${NC}"
else
    echo -e "${BLUE}Installation d'OpenJDK 21...${NC}"
    apt-get update -y
    apt-get install -y openjdk-21-jre-headless curl
fi

echo -e "${BLUE}=== [3/6] Création de l'utilisateur système 'prospecta' ===${NC}"
if ! id "prospecta" &>/dev/null; then
    useradd -r -s /bin/false -d /opt/prospecta prospecta
    echo -e "${GREEN}Utilisateur système 'prospecta' créé avec succès.${NC}"
else
    echo "L'utilisateur 'prospecta' existe déjà."
fi

echo -e "${BLUE}=== [4/6] Création de l'arborescence /opt/prospecta ===${NC}"
mkdir -p /opt/prospecta/staging
mkdir -p /opt/prospecta/production
mkdir -p /opt/prospecta/backups/staging
mkdir -p /opt/prospecta/backups/production

# Copie des fichiers .env par défaut si absents
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ ! -f /opt/prospecta/staging/prospecta-staging.env ]; then
    if [ -f "${SCRIPT_DIR}/env/prospecta-staging.env.example" ]; then
        cp "${SCRIPT_DIR}/env/prospecta-staging.env.example" /opt/prospecta/staging/prospecta-staging.env
        echo -e "${GREEN}Template .env Staging copié vers /opt/prospecta/staging/prospecta-staging.env${NC}"
    fi
fi

if [ ! -f /opt/prospecta/production/prospecta-prod.env ]; then
    if [ -f "${SCRIPT_DIR}/env/prospecta-prod.env.example" ]; then
        cp "${SCRIPT_DIR}/env/prospecta-prod.env.example" /opt/prospecta/production/prospecta-prod.env
        echo -e "${GREEN}Template .env Production copié vers /opt/prospecta/production/prospecta-prod.env${NC}"
    fi
fi

# Permissions sur les répertoires
chown -R prospecta:prospecta /opt/prospecta
chmod -R 750 /opt/prospecta

echo -e "${BLUE}=== [5/6] Installation des services systemd ===${NC}"
if [ -f "${SCRIPT_DIR}/systemd/prospecta-staging.service" ]; then
    cp "${SCRIPT_DIR}/systemd/prospecta-staging.service" /etc/systemd/system/
    echo "Service prospecta-staging installé."
fi

if [ -f "${SCRIPT_DIR}/systemd/prospecta-prod.service" ]; then
    cp "${SCRIPT_DIR}/systemd/prospecta-prod.service" /etc/systemd/system/
    echo "Service prospecta-prod installé."
fi

systemctl daemon-reload
systemctl enable prospecta-staging.service || true
systemctl enable prospecta-prod.service || true

echo -e "${BLUE}=== [6/6] Configuration sudoers pour le déploiement sans mot de passe ===${NC}"
# Permet aux utilisateurs jenkins ou deploy de redémarrer les services sans prompt sudo
cat << 'EOF' > /etc/sudoers.d/prospecta-deploy
# Autorisation sans mot de passe pour le redémarrage des services Prospecta
jenkins ALL=(ALL) NOPASSWD: /usr/bin/systemctl restart prospecta-staging, /usr/bin/systemctl restart prospecta-prod, /usr/bin/systemctl stop prospecta-*, /usr/bin/systemctl start prospecta-*, /usr/bin/systemctl status prospecta-*, /usr/bin/journalctl -u prospecta-*
deploy ALL=(ALL) NOPASSWD: /usr/bin/systemctl restart prospecta-staging, /usr/bin/systemctl restart prospecta-prod, /usr/bin/systemctl stop prospecta-*, /usr/bin/systemctl start prospecta-*, /usr/bin/systemctl status prospecta-*, /usr/bin/journalctl -u prospecta-*
EOF

chmod 440 /etc/sudoers.d/prospecta-deploy
echo -e "${GREEN}Configuration sudoers appliquée dans /etc/sudoers.d/prospecta-deploy.${NC}"

echo -e "\n${GREEN}==============================================================${NC}"
echo -e "${GREEN}  Initialisation du serveur 180.149.196.68 terminée avec succès !  ${NC}"
echo -e "${GREEN}  - Staging : /opt/prospecta/staging (Port: 8082)            ${NC}"
echo -e "${GREEN}  - Production : /opt/prospecta/production (Port: 8080)        ${NC}"
echo -e "${GREEN}==============================================================${NC}"
