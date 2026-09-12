// =============================================================================
// Jenkinsfile — Pipeline CI/CD Prospecta Backend
// Déploiement automatisé Staging & Production sur le serveur 180.149.196.68
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
            choices: ['staging', 'production'],
            description: 'Environnement cible sur le serveur (staging = port 8082, production = port 8085)'
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
            description: 'Mode de déploiement: "ssh" (Jenkins distant vers 180.149.196.68) ou "local" (Jenkins tourne sur le serveur)'
        )
        string(
            name: 'SSH_CREDENTIALS_ID',
            defaultValue: 'prospecta-server-ssh',
            description: 'Identifiant du secret SSH configuré dans Jenkins (nécessaire en mode ssh)'
        )
        string(
            name: 'SSH_USER',
            defaultValue: 'root',
            description: 'Utilisateur SSH sur le serveur cible (ex: root, deploy ou jenkins)'
        )
    }

    environment {
        APP_NAME     = "prospecta-backend"
        JAR_FILE     = "target/prospecta-backend.jar"
        MAVEN_OPTS   = "-Duser.timezone=Africa/Dakar -Dfile.encoding=UTF-8"
        // Variables dynamiques définies lors de l'étape Init
        DEPLOY_PATH  = ""
        SERVICE_NAME = ""
        BACKUP_DIR   = ""
        HEALTH_URL   = ""
        PORT         = ""
    }

    stages {

        stage('Checkout') {
            steps {
                echo "=== Récupération du code source ==="
                checkout scm
            }
        }

        stage('Init & Configuration') {
            steps {
                script {
                    echo "=== Initialisation de la configuration pour : ${params.ENVIRONMENT} ==="

                    if (params.ENVIRONMENT == 'production') {
                        env.PORT         = "8085"
                        env.DEPLOY_PATH  = "/opt/prospecta/production/prospecta-backend.jar"
                        env.SERVICE_NAME = "prospecta-prod"
                        env.BACKUP_DIR   = "/opt/prospecta/backups/production/"
                    } else {
                        env.PORT         = "8082"
                        env.DEPLOY_PATH  = "/opt/prospecta/staging/prospecta-backend.jar"
                        env.SERVICE_NAME = "prospecta-staging"
                        env.BACKUP_DIR   = "/opt/prospecta/backups/staging/"
                    }

                    // En mode SSH, le health check peut s'exécuter via IP publique ou en local sur le serveur
                    env.HEALTH_URL = "http://${params.TARGET_HOST}:${env.PORT}/actuator/health"

                    echo "----------------------------------------------------"
                    echo " Application       : ${env.APP_NAME}"
                    echo " Environnement     : ${params.ENVIRONMENT}"
                    echo " Serveur Cible     : ${params.TARGET_HOST}"
                    echo " Port Application  : ${env.PORT}"
                    echo " Chemin Déploiement: ${env.DEPLOY_PATH}"
                    echo " Service Systemd   : ${env.SERVICE_NAME}"
                    echo " Dossier Backup    : ${env.BACKUP_DIR}"
                    echo " Endpoint Santé    : ${env.HEALTH_URL}"
                    echo " Mode Déploiement  : ${params.DEPLOY_MODE}"
                    echo "----------------------------------------------------"
                }
            }
        }

        stage('Build & Package') {
            steps {
                script {
                    echo "=== Compilation et packaging Maven (Java 21) ==="
                    def mavenCmd = "./mvnw"
                    if (!fileExists('./mvnw')) {
                        mavenCmd = "mvn"
                    }

                    if (params.SKIP_TESTS) {
                        echo "Build sans exécution des tests unitaires..."
                        sh "${mavenCmd} clean package -DskipTests"
                    } else {
                        echo "Build avec exécution des tests unitaires..."
                        sh "${mavenCmd} clean package"
                    }

                    // Vérification que le binaire JAR a bien été généré
                    sh "ls -lah target/*.jar"
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
                        message: "Confirmer le déploiement en PRODUCTION sur le serveur ${params.TARGET_HOST} ?",
                        ok: 'Valider et Déployer en Production',
                        submitterParameter: 'DEPLOYED_BY'
                    )
                }
                echo "Déploiement en production approuvé par : ${env.DEPLOYED_BY ?: 'Utilisateur autorisé'}"
            }
        }

        stage('Backup Current Version') {
            steps {
                script {
                    echo "=== Sauvegarde de la version active sur le serveur ==="

                    if (params.DEPLOY_MODE == 'ssh') {
                        sshagent(credentials: [params.SSH_CREDENTIALS_ID]) {
                            sh """
                            ssh -o StrictHostKeyChecking=no ${params.SSH_USER}@${params.TARGET_HOST} '
                                set +e
                                mkdir -p ${env.BACKUP_DIR}
                                if [ -f ${env.DEPLOY_PATH} ]; then
                                    BACKUP_NAME="prospecta_\$(date +%s).jar"
                                    cp ${env.DEPLOY_PATH} ${env.BACKUP_DIR}/\${BACKUP_NAME}
                                    echo "Backup effectué : ${env.BACKUP_DIR}/\${BACKUP_NAME}"
                                else
                                    echo "Aucune version existante à sauvegarder dans ${env.DEPLOY_PATH}"
                                fi
                            '
                            """
                        }
                    } else {
                        sh """
                        set +e
                        mkdir -p ${env.BACKUP_DIR}
                        if [ -f ${env.DEPLOY_PATH} ]; then
                            BACKUP_NAME="prospecta_\$(date +%s).jar"
                            cp ${env.DEPLOY_PATH} ${env.BACKUP_DIR}/\${BACKUP_NAME}
                            echo "Backup effectué : ${env.BACKUP_DIR}/\${BACKUP_NAME}"
                        else
                            echo "Aucune version existante à sauvegarder dans ${env.DEPLOY_PATH}"
                        fi
                        """
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                script {
                    echo "=== Déploiement du nouveau JAR vers ${params.TARGET_HOST} ==="

                    if (params.DEPLOY_MODE == 'ssh') {
                        sshagent(credentials: [params.SSH_CREDENTIALS_ID]) {
                            sh """
                            set -e
                            echo "Transfert sécurisé du JAR vers ${params.TARGET_HOST}:${env.DEPLOY_PATH}..."
                            scp -o StrictHostKeyChecking=no ${env.JAR_FILE} ${params.SSH_USER}@${params.TARGET_HOST}:${env.DEPLOY_PATH}

                            echo "Application des permissions..."
                            ssh -o StrictHostKeyChecking=no ${params.SSH_USER}@${params.TARGET_HOST} '
                                chown prospecta:prospecta ${env.DEPLOY_PATH} || true
                                chmod 640 ${env.DEPLOY_PATH} || true
                            '

                            echo "Redémarrage du service systemd : ${env.SERVICE_NAME}..."
                            ssh -o StrictHostKeyChecking=no ${params.SSH_USER}@${params.TARGET_HOST} '
                                sudo systemctl restart ${env.SERVICE_NAME}
                            '
                            """
                        }
                    } else {
                        sh """
                        set -e
                        echo "Copie du JAR vers ${env.DEPLOY_PATH}..."
                        cp ${env.JAR_FILE} ${env.DEPLOY_PATH}
                        chown prospecta:prospecta ${env.DEPLOY_PATH} || true
                        chmod 640 ${env.DEPLOY_PATH} || true

                        echo "Redémarrage du service systemd : ${env.SERVICE_NAME}..."
                        sudo systemctl restart ${env.SERVICE_NAME}
                        """
                    }
                }
            }
        }

        stage('Health Check') {
            steps {
                script {
                    echo "=== Vérification de l'état de l'application (/actuator/health) ==="

                    // Script de sondage de santé avec retry progressif
                    def checkHealthScript = """
                    #!/bin/bash
                    echo "Vérification sur : ${env.HEALTH_URL}"
                    MAX_ATTEMPTS=25
                    SLEEP_TIME=6

                    for i in \$(seq 1 \$MAX_ATTEMPTS); do
                        HTTP_CODE=\$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 ${env.HEALTH_URL} || true)
                        echo "Tentative \$i/\$MAX_ATTEMPTS - Code HTTP: \$HTTP_CODE"

                        if [ "\$HTTP_CODE" = "200" ]; then
                            BODY=\$(curl -s --connect-timeout 5 ${env.HEALTH_URL} || true)
                            echo "Réponse de santé : \$BODY"

                            if echo "\$BODY" | grep -q "UP"; then
                                echo "=================================================="
                                echo " Application démarrée avec succès (Statut: UP) ! "
                                echo "=================================================="
                                exit 0
                            fi
                        fi

                        echo "En attente du démarrage de l'application..."
                        sleep \$SLEEP_TIME
                    done

                    echo "ERREUR : L'application ne répond pas après \$MAX_ATTEMPTS tentatives."
                    exit 1
                    """

                    try {
                        sh checkHealthScript
                    } catch (Exception e) {
                        echo "=== Échec du Health Check, inspection des logs systemd ==="
                        if (params.DEPLOY_MODE == 'ssh') {
                            sshagent(credentials: [params.SSH_CREDENTIALS_ID]) {
                                sh "ssh -o StrictHostKeyChecking=no ${params.SSH_USER}@${params.TARGET_HOST} 'sudo journalctl -u ${env.SERVICE_NAME} -n 120 --no-pager' || true"
                            }
                        } else {
                            sh "sudo journalctl -u ${env.SERVICE_NAME} -n 120 --no-pager || true"
                        }
                        error("Déploiement échoué : Actuator Health n'a pas répondu UP.")
                    }
                }
            }
        }
    }

    post {

        failure {
            echo "=========================================================="
            echo " ALERTE : Échec détecté → Déclenchement du Rollback auto ! "
            echo "=========================================================="
            script {
                if (params.DEPLOY_MODE == 'ssh') {
                    sshagent(credentials: [params.SSH_CREDENTIALS_ID]) {
                        sh """
                        ssh -o StrictHostKeyChecking=no ${params.SSH_USER}@${params.TARGET_HOST} '
                            LAST_BACKUP=\$(ls -t ${env.BACKUP_DIR}/prospecta_*.jar 2>/dev/null | head -n 1)

                            if [ -n "\$LAST_BACKUP" ] && [ -f "\$LAST_BACKUP" ]; then
                                echo "Restauration depuis le backup : \$LAST_BACKUP"
                                cp \$LAST_BACKUP ${env.DEPLOY_PATH}
                                chown prospecta:prospecta ${env.DEPLOY_PATH} || true
                                sudo systemctl restart ${env.SERVICE_NAME}
                                echo "Rollback terminé avec succès."
                            else
                                echo "AVERTISSEMENT : Aucun fichier de sauvegarde trouvé dans ${env.BACKUP_DIR}."
                            fi
                        '
                        """
                    }
                } else {
                    sh """
                    LAST_BACKUP=\$(ls -t ${env.BACKUP_DIR}/prospecta_*.jar 2>/dev/null | head -n 1)

                    if [ -n "\$LAST_BACKUP" ] && [ -f "\$LAST_BACKUP" ]; then
                        echo "Restauration depuis le backup : \$LAST_BACKUP"
                        cp \$LAST_BACKUP ${env.DEPLOY_PATH}
                        chown prospecta:prospecta ${env.DEPLOY_PATH} || true
                        sudo systemctl restart ${env.SERVICE_NAME}
                        echo "Rollback terminé avec succès."
                    else
                        echo "AVERTISSEMENT : Aucun fichier de sauvegarde trouvé dans ${env.BACKUP_DIR}."
                    fi
                    """
                }
            }
        }

        success {
            echo "=========================================================="
            echo " SUCCÈS : Déploiement de Prospecta [${params.ENVIRONMENT}] "
            echo " Serveur  : ${params.TARGET_HOST}:${env.PORT}             "
            echo " Service  : ${env.SERVICE_NAME}                           "
            echo " Statut   : Application UP & Opérationnelle               "
            echo "=========================================================="
        }

        always {
            echo "Nettoyage de l'espace de travail..."
            cleanWs()
        }
    }
}
