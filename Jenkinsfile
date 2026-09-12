// =============================================================================
// Jenkinsfile — Pipeline CI/CD Prospecta Backend (Docker & Docker Compose)
// Déploiement automatisé Staging & Production sur le serveur 180.149.196.68
// Architecture conteneurisée sur le réseau 'infrastructure-network'
// =============================================================================

pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '15', artifactNumToKeepStr: '5'))
        timeout(time: 30, unit: 'MINUTES')
    }

    parameters {
        choice(
            name: 'ENVIRONMENT',
            choices: ['production', 'staging'],
            description: 'Environnement cible sur le serveur (production = port 8085, staging = port 8086)'
        )
        booleanParam(
            name: 'SKIP_TESTS',
            defaultValue: false,
            description: 'Cocher pour ignorer l\'exécution des tests unitaires Maven (-DskipTests)'
        )
        string(
            name: 'TARGET_HOST',
            defaultValue: '180.149.196.68',
            description: 'Adresse IP ou nom d\'hôte du serveur distant'
        )
        choice(
            name: 'DEPLOY_MODE',
            choices: ['ssh', 'local'],
            description: 'Mode de déploiement : "ssh" (Jenkins distant vers VPS) ou "local" (Jenkins tourne directement sur le VPS)'
        )
        string(
            name: 'SSH_CREDENTIALS_ID',
            defaultValue: 'prospecta-server-ssh',
            description: 'Identifiant du secret SSH configuré dans Jenkins (SSH Username with private key)'
        )
        string(
            name: 'SSH_USER',
            defaultValue: 'deploy',
            description: 'Utilisateur SSH sur le serveur cible (ex: deploy ou root)'
        )
        string(
            name: 'DOCKER_REGISTRY',
            defaultValue: '',
            description: 'Registry Docker optionnel (ex: registry.clatous.com ou laisser vide pour transfert direct SSH)'
        )
    }

    environment {
        APP_NAME           = "prospecta-backend"
        IMAGE_BASE         = "prospecta-backend"
        MAVEN_OPTS         = "-Duser.timezone=Africa/Dakar -Dfile.encoding=UTF-8"

        // Variables calculées dynamiquement dans l'étape Init
        IMAGE_NAME         = ""
        IMAGE_TAG          = ""
        FULL_IMAGE_NAME    = ""
        TARGET_PORT        = ""
        CONTAINER_NAME     = ""
        REMOTE_DIR         = ""
        COMPOSE_FILE       = ""
        ENV_FILE           = ""
        HEALTH_URL         = ""
        DEPLOY_EXECUTED    = false
        PREVIOUS_IMAGE_TAG = ""
    }

    stages {

        stage('Checkout') {
            steps {
                echo "=== 1. Récupération du code source ==="
                checkout scm
            }
        }

        stage('Init & Configuration') {
            steps {
                script {
                    echo "=== 2. Configuration pour l'environnement : ${params.ENVIRONMENT} ==="

                    if (params.ENVIRONMENT == 'production') {
                        env.TARGET_PORT    = "8085"
                        env.CONTAINER_NAME = "prospecta-backend-prod"
                        env.REMOTE_DIR     = "/opt/prospecta/production"
                        env.COMPOSE_FILE   = "docker-compose.prod.yml"
                        env.ENV_FILE       = "/opt/prospecta/production/prospecta-prod.env"
                        env.IMAGE_NAME     = "prospecta-backend"
                        env.IMAGE_TAG      = "prod-${BUILD_NUMBER}"
                    } else {
                        // Port 8086 pour éviter le conflit avec Keycloak qui occupe le port 8082
                        env.TARGET_PORT    = "8086"
                        env.CONTAINER_NAME = "prospecta-backend-staging"
                        env.REMOTE_DIR     = "/opt/prospecta/staging"
                        env.COMPOSE_FILE   = "docker-compose.staging.yml"
                        env.ENV_FILE       = "/opt/prospecta/staging/prospecta-staging.env"
                        env.IMAGE_NAME     = "prospecta-backend-staging"
                        env.IMAGE_TAG      = "staging-${BUILD_NUMBER}"
                    }

                    if (params.DOCKER_REGISTRY && params.DOCKER_REGISTRY.trim() != '') {
                        env.FULL_IMAGE_NAME = "${params.DOCKER_REGISTRY.trim()}/${env.IMAGE_NAME}:${env.IMAGE_TAG}"
                    } else {
                        env.FULL_IMAGE_NAME = "${env.IMAGE_NAME}:${env.IMAGE_TAG}"
                    }

                    // Le healthcheck est testé en LOCAL sur le VPS (car les ports sont restreints à 127.0.0.1)
                    env.HEALTH_URL = "http://127.0.0.1:${env.TARGET_PORT}/actuator/health"

                    echo "----------------------------------------------------"
                    echo " Application       : ${env.APP_NAME}"
                    echo " Environnement     : ${params.ENVIRONMENT}"
                    echo " Serveur Cible     : ${params.TARGET_HOST}"
                    echo " Port VPS (Host)   : ${env.TARGET_PORT}"
                    echo " Nom Conteneur     : ${env.CONTAINER_NAME}"
                    echo " Image Docker      : ${env.FULL_IMAGE_NAME}"
                    echo " Dossier VPS       : ${env.REMOTE_DIR}"
                    echo " Compose File      : ${env.COMPOSE_FILE}"
                    echo " Env File (VPS)    : ${env.ENV_FILE}"
                    echo " Healthcheck VPS   : ${env.HEALTH_URL}"
                    echo " Mode Déploiement  : ${params.DEPLOY_MODE}"
                    echo "----------------------------------------------------"
                }
            }
        }

        stage('Inspect Running Container') {
            steps {
                script {
                    echo "=== 3. Détection de l'image actuellement active sur le serveur ==="
                    def detectCmd = "docker inspect --format '{{.Config.Image}}' ${env.CONTAINER_NAME} 2>/dev/null || true"

                    try {
                        if (params.DEPLOY_MODE == 'ssh') {
                            sshagent(credentials: [params.SSH_CREDENTIALS_ID]) {
                                env.PREVIOUS_IMAGE_TAG = sh(
                                    script: "ssh -o StrictHostKeyChecking=no ${params.SSH_USER}@${params.TARGET_HOST} \"${detectCmd}\"",
                                    returnStdout: true
                                ).trim()
                            }
                        } else {
                            env.PREVIOUS_IMAGE_TAG = sh(
                                script: detectCmd,
                                returnStdout: true
                            ).trim()
                        }

                        if (env.PREVIOUS_IMAGE_TAG && env.PREVIOUS_IMAGE_TAG != '') {
                            echo "Image actuellement en cours d'exécution : ${env.PREVIOUS_IMAGE_TAG}"
                        } else {
                            echo "Aucun conteneur actif détecté (premier déploiement ou conteneur arrêté)."
                        }
                    } catch (Exception e) {
                        echo "Avertissement lors de la détection de l'image active : ${e.getMessage()}"
                    }
                }
            }
        }

        stage('Build & Test Maven') {
            steps {
                script {
                    echo "=== 4. Compilation Maven et exécution des tests (Java 21) ==="
                    def mavenCmd = fileExists('./mvnw') ? "./mvnw" : "mvn"

                    if (params.SKIP_TESTS) {
                        echo "Build sans tests unitaires (-DskipTests)..."
                        sh "${mavenCmd} clean package -DskipTests"
                    } else {
                        echo "Build avec exécution des tests unitaires..."
                        sh "${mavenCmd} clean package"
                    }

                    // Vérification que le binaire JAR a bien été généré dans target/
                    sh "ls -lah target/*.jar"
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    echo "=== 5. Construction de l'image Docker [${env.FULL_IMAGE_NAME}] ==="
                    sh "docker build -t ${env.FULL_IMAGE_NAME} -t ${env.IMAGE_NAME}:latest ."
                    echo "Vérification locale de l'image créée :"
                    sh "docker images | grep ${env.IMAGE_NAME} | head -n 5"
                }
            }
        }

        stage('Approval Gate (Production)') {
            when {
                expression { params.ENVIRONMENT == 'production' }
            }
            steps {
                timeout(time: 30, unit: 'MINUTES') {
                    input(
                        id: 'ApproveProductionDeploy',
                        message: "Confirmer le déploiement DOCKER en PRODUCTION sur le serveur ${params.TARGET_HOST} (Port ${env.TARGET_PORT}) ?",
                        ok: 'Valider et Déployer en Production',
                        submitterParameter: 'DEPLOYED_BY'
                    )
                }
                echo "Déploiement en production approuvé par : ${env.DEPLOYED_BY ?: 'Utilisateur autorisé'}"
            }
        }

        stage('Deploy to Server') {
            steps {
                script {
                    echo "=== 6. Déploiement Docker Compose sur ${params.TARGET_HOST} ==="
                    env.DEPLOY_EXECUTED = true

                    def prepCmd = """
                        set -e
                        mkdir -p ${env.REMOTE_DIR}
                        # Création du réseau Docker s'il n'existe pas encore
                        docker network create infrastructure-network 2>/dev/null || true

                        if [ ! -f ${env.ENV_FILE} ]; then
                            echo "AVERTISSEMENT: Le fichier d'environnement ${env.ENV_FILE} est manquant sur le serveur !"
                        fi
                    """

                    def composeSource = "deploy/${env.COMPOSE_FILE}"

                    if (params.DEPLOY_MODE == 'ssh') {
                        sshagent(credentials: [params.SSH_CREDENTIALS_ID]) {
                            echo "Préparation du serveur ${params.TARGET_HOST}..."
                            sh "ssh -o StrictHostKeyChecking=no ${params.SSH_USER}@${params.TARGET_HOST} '${prepCmd}'"

                            echo "Copie du descripteur Docker Compose vers ${env.REMOTE_DIR}/docker-compose.yml..."
                            sh "scp -o StrictHostKeyChecking=no ${composeSource} ${params.SSH_USER}@${params.TARGET_HOST}:${env.REMOTE_DIR}/docker-compose.yml"

                            if (params.DOCKER_REGISTRY && params.DOCKER_REGISTRY.trim() != '') {
                                echo "Push de l'image vers le Registry Docker..."
                                sh "docker push ${env.FULL_IMAGE_NAME}"
                                echo "Pull de l'image sur le VPS..."
                                sh "ssh -o StrictHostKeyChecking=no ${params.SSH_USER}@${params.TARGET_HOST} 'docker pull ${env.FULL_IMAGE_NAME}'"
                            } else {
                                echo "Transfert direct de l'image Docker via streaming SSH (docker save | gzip | docker load)..."
                                sh "docker save ${env.FULL_IMAGE_NAME} | gzip -c | ssh -o StrictHostKeyChecking=no ${params.SSH_USER}@${params.TARGET_HOST} 'gunzip -c | docker load'"
                            }

                            echo "Démarrage du conteneur avec Docker Compose..."
                            sh """
                            ssh -o StrictHostKeyChecking=no ${params.SSH_USER}@${params.TARGET_HOST} '
                                cd ${env.REMOTE_DIR}
                                IMAGE_TAG=${env.IMAGE_TAG} docker compose up -d --remove-orphans
                            '
                            """
                        }
                    } else {
                        // Déploiement en mode local (Jenkins s'exécute directement sur le VPS)
                        sh prepCmd
                        sh "cp ${composeSource} ${env.REMOTE_DIR}/docker-compose.yml"

                        echo "Démarrage du conteneur avec Docker Compose en local..."
                        sh """
                        cd ${env.REMOTE_DIR}
                        IMAGE_TAG=${env.IMAGE_TAG} docker compose up -d --remove-orphans
                        """
                    }
                }
            }
        }

        stage('Health Check') {
            steps {
                script {
                    echo "=== 7. Vérification de santé de l'application (/actuator/health) sur le VPS ==="

                    // Le test est exécuté directement SUR LE SERVEUR via 127.0.0.1:${TARGET_PORT}
                    def checkHealthScript = """
                    set +e
                    echo "Sondage de l'état de santé sur : ${env.HEALTH_URL}"
                    MAX_ATTEMPTS=25
                    SLEEP_TIME=5

                    for i in \$(seq 1 \$MAX_ATTEMPTS); do
                        HTTP_CODE=\$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 4 ${env.HEALTH_URL} || true)
                        echo "Tentative \$i/\$MAX_ATTEMPTS - Code HTTP: \$HTTP_CODE"

                        if [ "\$HTTP_CODE" = "200" ]; then
                            BODY=\$(curl -s --connect-timeout 4 ${env.HEALTH_URL} || true)
                            echo "Réponse Actuator : \$BODY"

                            if echo "\$BODY" | grep -q "UP"; then
                                echo "=================================================="
                                echo " Conteneur ${env.CONTAINER_NAME} démarré avec succès (Statut: UP) ! "
                                echo "=================================================="
                                exit 0
                            fi
                        fi

                        echo "En attente du démarrage complet de Spring Boot..."
                        sleep \$SLEEP_TIME
                    done

                    echo "ERREUR : L'application n'a pas répondu UP après \$MAX_ATTEMPTS tentatives."
                    exit 1
                    """

                    try {
                        if (params.DEPLOY_MODE == 'ssh') {
                            sshagent(credentials: [params.SSH_CREDENTIALS_ID]) {
                                sh "ssh -o StrictHostKeyChecking=no ${params.SSH_USER}@${params.TARGET_HOST} '${checkHealthScript}'"
                            }
                        } else {
                            sh checkHealthScript
                        }
                    } catch (Exception e) {
                        echo "=== ÉCHEC DU HEALTH CHECK ! Inspection des 100 dernières lignes de logs du conteneur ==="
                        def logsCmd = "docker logs ${env.CONTAINER_NAME} --tail 100 --timestamps"

                        if (params.DEPLOY_MODE == 'ssh') {
                            sshagent(credentials: [params.SSH_CREDENTIALS_ID]) {
                                sh "ssh -o StrictHostKeyChecking=no ${params.SSH_USER}@${params.TARGET_HOST} '${logsCmd}' || true"
                            }
                        } else {
                            sh "${logsCmd} || true"
                        }
                        error("Déploiement interrompu : L'application conteneurisée n'a pas répondu UP au Health Check.")
                    }
                }
            }
        }
    }

    post {

        failure {
            echo "=========================================================="
            echo " ALERTE : Échec détecté lors de l'exécution du Pipeline ! "
            echo "=========================================================="
            script {
                // Le rollback ne se déclenche que si le déploiement a effectivement commencé
                // et qu'une version précédente valide avait été détectée
                if (env.DEPLOY_EXECUTED == 'true' && env.PREVIOUS_IMAGE_TAG != '') {
                    echo "Déclenchement du Rollback automatique vers l'ancienne image : ${env.PREVIOUS_IMAGE_TAG}"

                    def rollbackCmd = """
                        set +e
                        cd ${env.REMOTE_DIR}
                        echo "Arrêt du conteneur défaillant..."
                        docker compose stop || true

                        echo "Restauration de la version précédente [${env.PREVIOUS_IMAGE_TAG}]..."
                        # Si le tag précédent est identifiable, relance via compose ou docker run
                        IMAGE_TAG=\$(echo "${env.PREVIOUS_IMAGE_TAG}" | awk -F':' '{print \$2}')
                        if [ -n "\$IMAGE_TAG" ]; then
                            IMAGE_TAG=\$IMAGE_TAG docker compose up -d
                        else
                            docker run -d --name ${env.CONTAINER_NAME} \
                                --restart unless-stopped \
                                --network infrastructure-network \
                                --env-file ${env.ENV_FILE} \
                                -p 127.0.0.1:${env.TARGET_PORT}:8085 \
                                ${env.PREVIOUS_IMAGE_TAG}
                        fi
                        echo "Rollback appliqué avec succès."
                    """

                    if (params.DEPLOY_MODE == 'ssh') {
                        sshagent(credentials: [params.SSH_CREDENTIALS_ID]) {
                            sh "ssh -o StrictHostKeyChecking=no ${params.SSH_USER}@${params.TARGET_HOST} '${rollbackCmd}'"
                        }
                    } else {
                        sh rollbackCmd
                    }
                } else {
                    echo "Aucun rollback nécessaire (le déploiement n'avait pas été initié ou aucune version antérieure n'existait)."
                }
            }
        }

        success {
            echo "=========================================================="
            echo " SUCCÈS : Déploiement Docker Prospecta terminé !          "
            echo " Environnement : ${params.ENVIRONMENT}                     "
            echo " Serveur       : ${params.TARGET_HOST}:${env.TARGET_PORT} (Local) "
            echo " Conteneur     : ${env.CONTAINER_NAME}                    "
            echo " Image Docker  : ${env.FULL_IMAGE_NAME}                   "
            echo " Statut        : Application UP & Opérationnelle           "
            echo "=========================================================="

            // Nettoyage des anciennes images Docker orphelines sur le VPS pour libérer le disque
            script {
                def pruneCmd = "docker image prune -f --filter 'until=48h' || true"
                try {
                    if (params.DEPLOY_MODE == 'ssh') {
                        sshagent(credentials: [params.SSH_CREDENTIALS_ID]) {
                            sh "ssh -o StrictHostKeyChecking=no ${params.SSH_USER}@${params.TARGET_HOST} '${pruneCmd}'"
                        }
                    } else {
                        sh pruneCmd
                    }
                } catch (Exception ignored) {
                }
            }
        }

        always {
            echo "Nettoyage de l'espace de travail Jenkins..."
            cleanWs()
        }
    }
}
