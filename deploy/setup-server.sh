#!/usr/bin/env bash
# ==============================================================================
# PROSPECTA - Script d'Initialisation du Serveur Cible (180.149.196.68)
# Prépare l'environnement Docker, le réseau 'infrastructure-network',
# les répertoires /opt/prospecta, les fichiers Compose et les permissions.
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

echo -e "${BLUE}=== [2/6] Vérification du moteur Docker et Docker Compose ===${NC}"
if command -v docker >/dev/null 2>&1; then
    DOCKER_VER=$(docker --version)
    echo -e "${GREEN}Docker détecté : ${DOCKER_VER}${NC}"
else
    echo -e "${BLUE}Installation de Docker Engine...${NC}"
    apt-get update -y
    apt-get install -y ca-certificates curl gnupg lsb-release
    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    chmod a+r /etc/apt/keyrings/docker.gpg
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
    apt-get update -y
    apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
fi

# Démarrer Docker au boot
systemctl enable docker
systemctl start docker

echo -e "${BLUE}=== [3/6] Création du réseau Docker 'infrastructure-network' ===${NC}"
if ! docker network ls --format '{{.Name}}' | grep -q "^infrastructure-network$"; then
    docker network create infrastructure-network
    echo -e "${GREEN}Réseau Docker 'infrastructure-network' créé.${NC}"
else
    echo "Le réseau Docker 'infrastructure-network' existe déjà."
fi

echo -e "${BLUE}=== [4/6] Création de l'arborescence /opt/prospecta ===${NC}"
mkdir -p /opt/prospecta/staging
mkdir -p /opt/prospecta/production
mkdir -p /opt/prospecta/backups/staging
mkdir -p /opt/prospecta/backups/production

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Copie des fichiers compose par défaut
if [ -f "${SCRIPT_DIR}/docker-compose.prod.yml" ]; then
    cp "${SCRIPT_DIR}/docker-compose.prod.yml" /opt/prospecta/production/docker-compose.yml
    echo -e "${GREEN}docker-compose.yml Production installé.${NC}"
fi

if [ -f "${SCRIPT_DIR}/docker-compose.staging.yml" ]; then
    cp "${SCRIPT_DIR}/docker-compose.staging.yml" /opt/prospecta/staging/docker-compose.yml
    echo -e "${GREEN}docker-compose.yml Staging installé.${NC}"
fi

# Copie des fichiers .env par défaut si absents
if [ ! -f /opt/prospecta/staging/prospecta-staging.env ]; then
    if [ -f "${SCRIPT_DIR}/env/prospecta-staging.env.example" ]; then
        cp "${SCRIPT_DIR}/env/prospecta-staging.env.example" /opt/prospecta/staging/prospecta-staging.env
        echo -e "${GREEN}Template .env Staging copié vers /opt/prospecta/staging/prospecta-staging.env${NC}"
    fi
fi

if [ ! -f /opt/prospecta/production/prospecta-prod.env ]; then
    if [ -f "${SCRIPT_DIR}/env/prospecta-prod.env" ]; then
        cp "${SCRIPT_DIR}/env/prospecta-prod.env" /opt/prospecta/production/prospecta-prod.env
        echo -e "${GREEN}Template .env Production copié vers /opt/prospecta/production/prospecta-prod.env${NC}"
    elif [ -f "${SCRIPT_DIR}/env/prospecta-prod.env.example" ]; then
        cp "${SCRIPT_DIR}/env/prospecta-prod.env.example" /opt/prospecta/production/prospecta-prod.env
        echo -e "${GREEN}Template .env Production copié vers /opt/prospecta/production/prospecta-prod.env${NC}"
    fi
fi

# Permissions sur les fichiers d'environnement
chmod 600 /opt/prospecta/*/*.env 2>/dev/null || true

echo -e "${BLUE}=== [5/6] Configuration utilisateur et groupe Docker ===${NC}"
# Création de l'utilisateur deploy si non existant
if ! id "deploy" &>/dev/null; then
    useradd -m -s /bin/bash deploy || true
    echo -e "${GREEN}Utilisateur 'deploy' créé.${NC}"
fi
usermod -aG docker deploy 2>/dev/null || true
if id "jenkins" &>/dev/null; then
    usermod -aG docker jenkins 2>/dev/null || true
fi

echo -e "${BLUE}=== [6/6] Configuration sudoers pour le déploiement Docker ===${NC}"
cat << 'EOF' > /etc/sudoers.d/prospecta-deploy
# Permissions sudo restreintes pour le déploiement Prospecta si nécessaire
deploy ALL=(ALL) NOPASSWD: /usr/bin/docker, /usr/bin/docker compose
jenkins ALL=(ALL) NOPASSWD: /usr/bin/docker, /usr/bin/docker compose
EOF

chmod 440 /etc/sudoers.d/prospecta-deploy

echo -e "\n${GREEN}==============================================================${NC}"
echo -e "${GREEN}  Initialisation Docker du serveur 180.149.196.68 terminée !   ${NC}"
echo -e "${GREEN}  - Réseau Docker : infrastructure-network                    ${NC}"
echo -e "${GREEN}  - Staging       : /opt/prospecta/staging (Port: 8086)       ${NC}"
echo -e "${GREEN}  - Production    : /opt/prospecta/production (Port: 8085)    ${NC}"
echo -e "${GREEN}==============================================================${NC}"
