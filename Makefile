# ==============================================================================
# Makefile - Prospecta Backend & Infrastructure
# Plateforme SaaS B2B de Prospection Commerciale & Automatisation des Ventes
# ==============================================================================

.PHONY: help dev build clean run run-dev run-prod \
        infra-up infra-down infra-restart infra-ps infra-logs infra-clean \
        db db-up db-down db-restart db-logs db-psql db-reset db-status \
        redis-up redis-down redis-logs redis-cli \
        kafka-up kafka-down kafka-logs \
        keycloak-up keycloak-down keycloak-logs \
        mailpit-up mailpit-down mailpit-logs mailpit-open \
        test test-e2e test-sec test-crm test-campaign test-whatsapp test-pipeline test-analytics \
        stop

# ------------------------------------------------------------------------------
# Détecteur Maven (privilégie ./mvnw si présent, sinon binaire système mvn)
# ------------------------------------------------------------------------------
MVN := $(shell if [ -f ./mvnw ]; then echo "./mvnw"; else echo "mvn"; fi)

# ------------------------------------------------------------------------------
# Docker Compose & Fichiers de configuration
# ------------------------------------------------------------------------------
COMPOSE_FILE       := docker-compose.yml
COMPOSE_PROJECT    := prospecta
DOCKER_COMPOSE     := docker compose -f $(COMPOSE_FILE) -p $(COMPOSE_PROJECT)

# ------------------------------------------------------------------------------
# Variables par défaut (valeurs de repli si .env ou .env.local sont absents)
# ------------------------------------------------------------------------------
SPRING_PROFILES_ACTIVE ?= local
DATABASE_HOST          ?= localhost
DATABASE_PORT          ?= 5435
DATABASE_NAME          ?= prospecta_db
DATABASE_USERNAME      ?= prospecta
DATABASE_PASSWORD      ?= prospecta_dev_password

REDIS_HOST             ?= localhost
REDIS_PORT             ?= 6379

KEYCLOAK_PORT          ?= 8081
MAILPIT_WEB_PORT       ?= 8025

# ------------------------------------------------------------------------------
# Inclusion des variables d'environnement locales
# ------------------------------------------------------------------------------
-include .env.local
-include .env
export

# ==============================================================================
# COULEURS & FORMATAGE TERMINAL
# ==============================================================================
CYAN    := \033[36m
GREEN   := \033[32m
YELLOW  := \033[33m
BLUE    := \033[34m
MAGENTA := \033[35m
BOLD    := \033[1m
RESET   := \033[0m

# ==============================================================================
# CIBLE PAR DÉFAUT : AIDE INTERACTIVE
# ==============================================================================
help:
	@echo ""
	@echo "$(BOLD)$(CYAN)==============================================================================$(RESET)"
	@echo "$(BOLD)$(CYAN)                  PROSPECTA BACKEND - GESTION INFRA & DOCKER                  $(RESET)"
	@echo "$(BOLD)$(CYAN)==============================================================================$(RESET)"
	@echo ""
	@echo "$(BOLD)$(YELLOW)🚀 DÉVELOPPEMENT & APPLICATION :$(RESET)"
	@echo "  $(GREEN)make dev$(RESET)             Démarre l'infra requise, attend la DB, compile et lance le backend"
	@echo "  $(GREEN)make run$(RESET)             Lance l'application Spring Boot (profil: $(SPRING_PROFILES_ACTIVE))"
	@echo "  $(GREEN)make run-dev$(RESET)         Lance l'application avec le profil 'dev'"
	@echo "  $(GREEN)make run-prod$(RESET)        Lance l'application avec le profil 'prod'"
	@echo "  $(GREEN)make build$(RESET)           Compile et génère le package JAR (sans exécuter les tests)"
	@echo "  $(GREEN)make clean$(RESET)           Nettoie le répertoire target/ généré par Maven"
	@echo ""
	@echo "$(BOLD)$(YELLOW)🐳 INFRASTRUCTURE COMPLÈTE (docker-compose) :$(RESET)"
	@echo "  $(GREEN)make infra-up$(RESET)        Démarre tous les conteneurs (Postgres, Redis, Kafka, Keycloak, Mailpit)"
	@echo "  $(GREEN)make infra-down$(RESET)      Arrête et supprime tous les conteneurs d'infrastructure"
	@echo "  $(GREEN)make infra-restart$(RESET)   Redémarre tous les services d'infrastructure"
	@echo "  $(GREEN)make infra-ps$(RESET)        Affiche l'état et les ports de tous les conteneurs"
	@echo "  $(GREEN)make infra-logs$(RESET)      Affiche les logs en continu de tous les conteneurs"
	@echo "  $(GREEN)make infra-clean$(RESET)     Arrête et PURGE tous les conteneurs ET volumes de données"
	@echo ""
	@echo "$(BOLD)$(YELLOW)🐘 POSTGRESQL (Base de données) :$(RESET)"
	@echo "  $(GREEN)make db$(RESET)              S'assure que le conteneur PostgreSQL est démarré et prêt"
	@echo "  $(GREEN)make db-down$(RESET)         Arrête le conteneur PostgreSQL"
	@echo "  $(GREEN)make db-restart$(RESET)      Redémarre PostgreSQL"
	@echo "  $(GREEN)make db-logs$(RESET)         Affiche les logs du conteneur PostgreSQL"
	@echo "  $(GREEN)make db-psql$(RESET)         Ouvre une session interactive psql dans le conteneur"
	@echo "  $(GREEN)make db-reset$(RESET)        Réinitialise la base de données $(DATABASE_NAME)"
	@echo ""
	@echo "$(BOLD)$(YELLOW)⚡ SERVICES AUXILIAIRES :$(RESET)"
	@echo "  $(GREEN)make redis-up$(RESET)        Démarre Redis (cache & verrous distribués)"
	@echo "  $(GREEN)make redis-cli$(RESET)       Ouvre un shell interactif redis-cli"
	@echo "  $(GREEN)make kafka-up$(RESET)        Démarre Apache Kafka (KRaft mode)"
	@echo "  $(GREEN)make kafka-logs$(RESET)      Affiche les logs de Kafka"
	@echo "  $(GREEN)make keycloak-up$(RESET)     Démarre Keycloak (port: $(KEYCLOAK_PORT), import realm automatique)"
	@echo "  $(GREEN)make keycloak-logs$(RESET)   Affiche les logs de Keycloak"
	@echo "  $(GREEN)make mailpit-up$(RESET)      Démarre Mailpit (SMTP: 1025, Web UI: $(MAILPIT_WEB_PORT))"
	@echo "  $(GREEN)make mailpit-open$(RESET)    Affiche l'URL d'accès à l'interface Web Mailpit"
	@echo ""
	@echo "$(BOLD)$(YELLOW)🧪 TESTS AUTOMATISÉS :$(RESET)"
	@echo "  $(GREEN)make test$(RESET)            Exécute l'intégralité de la suite de tests unitaires & intégration"
	@echo "  $(GREEN)make test-e2e$(RESET)        Exécute le test Golden Path E2E (21 étapes commerciales)"
	@echo "  $(GREEN)make test-sec$(RESET)        Exécute les tests de sécurité (Multi-tenancy & SSRF)"
	@echo "  $(GREEN)make test-crm$(RESET)        Exécute les tests CRM & Moteur de Scoring Lead Sénégal"
	@echo "  $(GREEN)make test-campaign$(RESET)   Exécute les tests du moteur de séquences de prospection"
	@echo "  $(GREEN)make test-whatsapp$(RESET)   Exécute les tests Webhook Meta WhatsApp & Unified Inbox"
	@echo "  $(GREEN)make test-pipeline$(RESET)   Exécute les tests CRM Pipeline & Opportunités d'affaires"
	@echo "  $(GREEN)make test-analytics$(RESET)  Exécute les tests d'Analytics & Métriques de conversion"
	@echo "$(BOLD)$(CYAN)==============================================================================$(RESET)"
	@echo ""

# ==============================================================================
# DÉVELOPPEMENT LOCAL BACKEND
# ==============================================================================
dev: db redis-up mailpit-up
	@echo "$(BLUE)Vérification de la disponibilité de PostgreSQL...$(RESET)"
	@until (command -v pg_isready > /dev/null 2>&1 && pg_isready -h $(DATABASE_HOST) -p $(DATABASE_PORT) -U $(DATABASE_USERNAME) > /dev/null 2>&1) || (! command -v pg_isready > /dev/null 2>&1 && docker exec prospecta-postgres pg_isready -U $(DATABASE_USERNAME) -d $(DATABASE_NAME) > /dev/null 2>&1); do \
		echo "$(YELLOW)PostgreSQL en cours d'initialisation sur le port $(DATABASE_PORT), attente 2s...$(RESET)"; \
		sleep 2; \
	done
	@echo "$(GREEN)Infrastructure minimale prête ! Compilation et lancement de Prospecta Backend...$(RESET)"
	$(MVN) clean compile
	$(MVN) spring-boot:run

build:
	@echo "$(BLUE)Compilation et packaging du projet...$(RESET)"
	$(MVN) clean package -DskipTests

clean:
	@echo "$(YELLOW)Nettoyage des artefacts de compilation...$(RESET)"
	$(MVN) clean

run:
	@echo "$(GREEN)Démarrage du backend Spring Boot (profil: $(SPRING_PROFILES_ACTIVE))...$(RESET)"
	$(MVN) spring-boot:run

run-dev:
	@echo "$(GREEN)Démarrage du backend Spring Boot (profil: dev)...$(RESET)"
	$(MVN) spring-boot:run -Dspring-boot.run.profiles=dev

run-prod:
	@echo "$(GREEN)Démarrage du backend Spring Boot (profil: prod)...$(RESET)"
	$(MVN) spring-boot:run -Dspring-boot.run.profiles=prod

# ==============================================================================
# GESTION GLOBALE DE L'INFRASTRUCTURE DOCKER
# ==============================================================================
infra-up:
	@echo "$(BLUE)Démarrage de tous les services d'infrastructure Prospecta...$(RESET)"
	$(DOCKER_COMPOSE) up -d
	@echo "$(GREEN)Tous les services d'infrastructure sont démarrés !$(RESET)"
	@$(MAKE) infra-ps

infra-down:
	@echo "$(YELLOW)Arrêt des services d'infrastructure...$(RESET)"
	$(DOCKER_COMPOSE) down

infra-restart:
	@echo "$(YELLOW)Redémarrage des services d'infrastructure...$(RESET)"
	$(DOCKER_COMPOSE) restart

infra-ps:
	@echo "$(CYAN)État des conteneurs Prospecta :$(RESET)"
	$(DOCKER_COMPOSE) ps

infra-logs:
	$(DOCKER_COMPOSE) logs -f

infra-clean:
	@echo "$(YELLOW)Attention: Arrêt et suppression des conteneurs ET volumes...$(RESET)"
	$(DOCKER_COMPOSE) down -v --remove-orphans

# ==============================================================================
# GESTION CIBLÉE : POSTGRESQL
# ==============================================================================
db: db-up

db-up:
	@echo "$(BLUE)Vérification et démarrage du conteneur PostgreSQL...$(RESET)"
	$(DOCKER_COMPOSE) up -d postgres
	@until (command -v pg_isready > /dev/null 2>&1 && pg_isready -h $(DATABASE_HOST) -p $(DATABASE_PORT) -U $(DATABASE_USERNAME) > /dev/null 2>&1) || (! command -v pg_isready > /dev/null 2>&1 && docker exec prospecta-postgres pg_isready -U $(DATABASE_USERNAME) -d $(DATABASE_NAME) > /dev/null 2>&1); do \
		echo "$(YELLOW)En attente de PostgreSQL sur le port $(DATABASE_PORT)...$(RESET)"; \
		sleep 1; \
	done
	@echo "$(GREEN)PostgreSQL est opérationnel sur le port $(DATABASE_PORT) (Base: $(DATABASE_NAME))$(RESET)"

db-down:
	@echo "$(YELLOW)Arrêt du conteneur PostgreSQL...$(RESET)"
	$(DOCKER_COMPOSE) stop postgres

db-restart:
	@echo "$(YELLOW)Redémarrage du conteneur PostgreSQL...$(RESET)"
	$(DOCKER_COMPOSE) restart postgres

db-logs:
	docker logs -f prospecta-postgres

db-psql:
	@echo "$(BLUE)Connexion psql au conteneur prospecta-postgres...$(RESET)"
	docker exec -it prospecta-postgres psql -U $(DATABASE_USERNAME) -d $(DATABASE_NAME)

db-reset:
	@echo "$(YELLOW)Réinitialisation de la base de données $(DATABASE_NAME)...$(RESET)"
	docker exec -it prospecta-postgres psql -U $(DATABASE_USERNAME) -d postgres -c "DROP DATABASE IF EXISTS $(DATABASE_NAME);"
	docker exec -it prospecta-postgres psql -U $(DATABASE_USERNAME) -d postgres -c "CREATE DATABASE $(DATABASE_NAME) OWNER $(DATABASE_USERNAME);"
	@echo "$(GREEN)Base de données $(DATABASE_NAME) réinitialisée avec succès.$(RESET)"

db-status: infra-ps

stop: infra-down

# ==============================================================================
# GESTION CIBLÉE : REDIS
# ==============================================================================
redis-up:
	@if docker ps --format '{{.Names}}' | grep -q "^prospecta-redis$$"; then \
		echo "$(GREEN)Le conteneur prospecta-redis est déjà actif.$(RESET)"; \
	else \
		echo "$(BLUE)Démarrage de Redis...$(RESET)"; \
		$(DOCKER_COMPOSE) up -d redis; \
	fi

redis-down:
	@echo "$(YELLOW)Arrêt de Redis...$(RESET)"
	$(DOCKER_COMPOSE) stop redis

redis-logs:
	docker logs -f prospecta-redis

redis-cli:
	docker exec -it prospecta-redis redis-cli

# ==============================================================================
# GESTION CIBLÉE : APACHE KAFKA (KRaft)
# ==============================================================================
kafka-up:
	@echo "$(BLUE)Démarrage d'Apache Kafka (KRaft)...$(RESET)"
	$(DOCKER_COMPOSE) up -d kafka

kafka-down:
	@echo "$(YELLOW)Arrêt de Kafka...$(RESET)"
	$(DOCKER_COMPOSE) stop kafka

kafka-logs:
	docker logs -f prospecta-kafka

# ==============================================================================
# GESTION CIBLÉE : KEYCLOAK (OIDC / OAuth2)
# ==============================================================================
keycloak-up: db
	@echo "$(BLUE)Démarrage de Keycloak (Port: $(KEYCLOAK_PORT))...$(RESET)"
	$(DOCKER_COMPOSE) up -d keycloak
	@echo "$(GREEN)Keycloak démarré sur http://localhost:$(KEYCLOAK_PORT) (Admin: admin / admin)$(RESET)"

keycloak-down:
	@echo "$(YELLOW)Arrêt de Keycloak...$(RESET)"
	$(DOCKER_COMPOSE) stop keycloak

keycloak-logs:
	docker logs -f prospecta-keycloak

# ==============================================================================
# GESTION CIBLÉE : MAILPIT (Serveur SMTP / Web UI de test)
# ==============================================================================
mailpit-up:
	@if docker ps --format '{{.Names}}' | grep -q "^prospecta-mailpit$$"; then \
		echo "$(GREEN)Le conteneur prospecta-mailpit est déjà actif.$(RESET)"; \
	else \
		echo "$(BLUE)Démarrage de Mailpit...$(RESET)"; \
		$(DOCKER_COMPOSE) up -d mailpit; \
		echo "$(GREEN)Mailpit Web UI accessible sur http://localhost:$(MAILPIT_WEB_PORT)$(RESET)"; \
	fi

mailpit-down:
	@echo "$(YELLOW)Arrêt de Mailpit...$(RESET)"
	$(DOCKER_COMPOSE) stop mailpit

mailpit-logs:
	docker logs -f prospecta-mailpit

mailpit-open:
	@echo "$(CYAN)Interface Web Mailpit : http://localhost:$(MAILPIT_WEB_PORT)$(RESET)"
	@echo "$(CYAN)Port SMTP Mailpit     : localhost:1025$(RESET)"

# ==============================================================================
# TESTS AUTOMATISÉS (MAVEN)
# ==============================================================================
test:
	@echo "$(BLUE)Exécution de l'ensemble de la suite de tests...$(RESET)"
	$(MVN) test

test-e2e:
	@echo "$(BLUE)Exécution du test Golden Path E2E (21 étapes commerciales)...$(RESET)"
	$(MVN) test -Dtest=GoldenPathEndToEndTest

test-sec:
	@echo "$(BLUE)Exécution des tests de sécurité & multi-tenancy...$(RESET)"
	$(MVN) test -Dtest=MultiTenantSecurityTest,SsrfValidatorTest

test-crm:
	@echo "$(BLUE)Exécution des tests CRM & Scoring Lead...$(RESET)"
	$(MVN) test -Dtest=LeadScoringEngineTest,DeduplicationServiceTest,ProspectControllerTest

test-campaign:
	@echo "$(BLUE)Exécution des tests du moteur de campagnes & séquences...$(RESET)"
	$(MVN) test -Dtest=CampaignServiceTest,CampaignExecutionServiceTest,CampaignControllerTest

test-whatsapp:
	@echo "$(BLUE)Exécution des tests Meta WhatsApp Webhook & Inbox...$(RESET)"
	$(MVN) test -Dtest=WhatsAppWebhookControllerTest,ConversationServiceTest

test-pipeline:
	@echo "$(BLUE)Exécution des tests CRM Pipeline & Opportunités...$(RESET)"
	$(MVN) test -Dtest=PipelineServiceTest,OpportunityControllerTest

test-analytics:
	@echo "$(BLUE)Exécution des tests d'Analytics & Reporting...$(RESET)"
	$(MVN) test -Dtest=AnalyticsServiceTest,AnalyticsControllerTest
