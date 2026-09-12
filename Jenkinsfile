// =============================================================================
// Jenkinsfile — Pipeline CI/CD Prospecta Backend (Docker, GHCR & Compose)
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
            defaultValue: 'deploy',
            description: 'Identifiant du secret SSH configuré dans Jenkins (deploy)'
        )
        string(
            name: 'SSH_USER',
            defaultValue: 'deploy',
            description: 'Utilisateur SSH sur le serveur cible'
        )
        string(
            name: 'DOCKER_REGISTRY',
            defaultValue: 'ghcr.io/ndoucoumane',
            description: 'Registry Docker (GitHub Container Registry ghcr.io/ndoucoumane)'
        )
        string(
            name: 'REGISTRY_CREDENTIALS_ID',
            defaultValue: 'github-container-registry',
            description: 'Identifiant du secret GitHub Container Registry configuré dans Jenkins'
        )
    }

    environment {
        APP_NAME   = "prospecta-backend"
        MAVEN_OPTS = "-Duser.timezone=Africa/Dakar -Dfile.encoding=UTF-8"
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

                    def isStaging = (params.ENVIRONMENT == 'staging')
                    def rawRegistry = (params.DOCKER_REGISTRY && params.DOCKER_REGISTRY.trim() != '') ? params.DOCKER_REGISTRY.trim() : 'ghcr.io/ndoucoumane'
                    def registry = rawRegistry.replaceAll('/+$', '')

                    env.TARGET_PORT     = isStaging ? '8086' : '8085'
                    env.CONTAINER_NAME  = isStaging ? 'prospecta-backend-staging' : 'prospecta-backend-prod'
                    env.REMOTE_DIR      = isStaging ? '/opt/prospecta/staging' : '/opt/prospecta/production'
                    env.COMPOSE_FILE    = isStaging ? 'docker-compose.staging.yml' : 'docker-compose.prod.yml'
                    env.ENV_FILE        = isStaging ? '/opt/prospecta/staging/prospecta-staging.env' : '/opt/prospecta/production/prospecta-prod.env'
                    env.IMAGE_NAME      = "${registry}/prospecta-backend${isStaging ? '-staging' : ''}"
                    env.IMAGE_TAG       = "${isStaging ? 'staging' : 'prod'}-${env.BUILD_NUMBER}"
                    env.FULL_IMAGE_NAME = "${env.IMAGE_NAME}:${env.IMAGE_TAG}"
                    env.HEALTH_URL      = "http://127.0.0.1:${env.TARGET_PORT}/actuator/health"
                    env.DEPLOY_EXECUTED = 'false'
                    env.PREVIOUS_IMAGE  = ''

                    echo "----------------------------------------------------"
                    echo " Application        : ${env.APP_NAME}"
                    echo " Environnement      : ${params.ENVIRONMENT}"
                    echo " Serveur Cible      : ${params.TARGET_HOST}"
                    echo " Port VPS (Host)    : ${env.TARGET_PORT}"
                    echo " Nom Conteneur      : ${env.CONTAINER_NAME}"
                    echo " Image Complète     : ${env.FULL_IMAGE_NAME}"
                    echo " Dossier VPS        : ${env.REMOTE_DIR}"
                    echo " Fichier Compose    : ${env.COMPOSE_FILE}"
                    echo " Fichier Env (VPS)  : ${env.ENV_FILE}"
                    echo " Healthcheck VPS    : ${env.HEALTH_URL}"
                    echo " Mode Déploiement   : ${params.DEPLOY_MODE}"
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
                                env.PREVIOUS_IMAGE = sh(
                                    script: "ssh -o StrictHostKeyChecking=no ${params.SSH_USER}@${params.TARGET_HOST} \"${detectCmd}\"",
                                    returnStdout: true
                                ).trim()
                            }
                        } else {
                            env.PREVIOUS_IMAGE = sh(
                                script: detectCmd,
                                returnStdout: true
                            ).trim()
                        }

                        if (env.PREVIOUS_IMAGE && env.PREVIOUS_IMAGE != '') {
                            echo "Image actuellement active sur le serveur : ${env.PREVIOUS_IMAGE}"
                        } else {
                            echo "Aucun conteneur actif détecté (premier déploiement ou conteneur arrêté)."
                        }
                    } catch (Exception e) {
                        echo "Avertissement lors de la détection de l'image active : ${e.getMessage()}"
                        env.PREVIOUS_IMAGE = ""
                    }
                }
            }
        }

        stage('Build & Test Maven') {
            steps {
                script {
                    echo "=== 4. Compilation Maven et exécution des tests (Java 21) ==="
                    sh "chmod +x ./mvnw || true"

                    def mavenCmd = fileExists('./mvnw') ? "./mvnw" : "mvn"
                    if (params.SKIP_TESTS) {
                        echo "Build sans exécution des tests unitaires (-DskipTests)..."
                        sh "${mavenCmd} clean package -DskipTests"
                    } else {
                        echo "Build avec exécution de l'ensemble des tests unitaires..."
                        sh "${mavenCmd} clean package"
                    }

                    // Vérification que le binaire JAR a bien été généré dans target/
                    sh "ls -lah target/*.jar"
                }
            }
        }

        stage('Build & Push Docker Image') {
            steps {
                script {
                    echo "=== 5. Construction de l'image Docker [${env.FULL_IMAGE_NAME}] ==="
                    sh "docker build -t ${env.FULL_IMAGE_NAME} -t ${env.IMAGE_NAME}:latest ."

                    if (params.DOCKER_REGISTRY && params.DOCKER_REGISTRY.trim() != '') {
                        echo "Connexion et publication sur GitHub Container Registry (GHCR)..."
                        withCredentials([
                            usernamePassword(
                                credentialsId: params.REGISTRY_CREDENTIALS_ID,
                                usernameVariable: 'GHCR_USERNAME',
                                passwordVariable: 'GHCR_TOKEN'
                            )
                        ]) {
                            sh '''
                                set +x
                                echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USERNAME" --password-stdin
                                set -x
                            '''
                            sh "docker push ${env.FULL_IMAGE_NAME}"
                            sh "docker push ${env.IMAGE_NAME}:latest"
                        }
                        echo "Image publiée avec succès sur GHCR : ${env.FULL_IMAGE_NAME}"
                    } else {
                        echo "DOCKER_REGISTRY non configuré : transfert direct via flux SSH."
                    }
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
                    env.DEPLOY_EXECUTED = 'true'

                    def prepCmd = """
                        set -e
                        mkdir -p ${env.REMOTE_DIR}
                        # Création du réseau Docker partagé s'il n'existe pas encore
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
                                echo "Authentification GHCR et téléchargement de l'image sur le VPS..."
                                withCredentials([
                                    usernamePassword(
                                        credentialsId: params.REGISTRY_CREDENTIALS_ID,
                                        usernameVariable: 'GHCR_USERNAME',
                                        passwordVariable: 'GHCR_TOKEN'
                                    )
                                ]) {
                                    sh """
                                    set +x
                                    echo "\$GHCR_TOKEN" | ssh -o StrictHostKeyChecking=no ${params.SSH_USER}@${params.TARGET_HOST} 'docker login ghcr.io -u "${GHCR_USERNAME}" --password-stdin'
                                    set -x
                                    ssh -o StrictHostKeyChecking=no ${params.SSH_USER}@${params.TARGET_HOST} 'docker pull ${env.FULL_IMAGE_NAME}'
                                    """
                                }
                            } else {
                                echo "Transfert direct de l'image Docker via streaming SSH (docker save | gzip | docker load)..."
                                sh "docker save ${env.FULL_IMAGE_NAME} | gzip -c | ssh -o StrictHostKeyChecking=no ${params.SSH_USER}@${params.TARGET_HOST} 'gunzip -c | docker load'"
                            }

                            echo "Démarrage du conteneur avec Docker Compose sur le VPS..."
                            sh """
                            ssh -o StrictHostKeyChecking=no ${params.SSH_USER}@${params.TARGET_HOST} '
                                cd ${env.REMOTE_DIR}
                                FULL_IMAGE_NAME="${env.FULL_IMAGE_NAME}" IMAGE_NAME="${env.IMAGE_NAME}" IMAGE_TAG="${env.IMAGE_TAG}" docker compose up -d --remove-orphans
                            '
                            """
                        }
                    } else {
                        // Déploiement en mode local (Jenkins tourne directement sur le VPS)
                        sh prepCmd
                        sh "cp ${composeSource} ${env.REMOTE_DIR}/docker-compose.yml"

                        echo "Démarrage du conteneur avec Docker Compose en local..."
                        sh """
                        cd ${env.REMOTE_DIR}
                        FULL_IMAGE_NAME="${env.FULL_IMAGE_NAME}" IMAGE_NAME="${env.IMAGE_NAME}" IMAGE_TAG="${env.IMAGE_TAG}" docker compose up -d --remove-orphans
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
                    echo "Sondage de l'état de santé local sur : ${env.HEALTH_URL}"
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
                if (env.DEPLOY_EXECUTED == 'true' && env.PREVIOUS_IMAGE && env.PREVIOUS_IMAGE.trim() != '') {
                    echo "Déclenchement du Rollback automatique vers l'ancienne image : ${env.PREVIOUS_IMAGE}"

                    def rollbackCmd = """
                        set +e
                        cd ${env.REMOTE_DIR}
                        echo "Arrêt du conteneur défaillant..."
                        docker compose stop || true

                        echo "Restauration de la version précédente [${env.PREVIOUS_IMAGE}]..."
                        FULL_IMAGE_NAME="${env.PREVIOUS_IMAGE}" docker compose up -d --remove-orphans
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
