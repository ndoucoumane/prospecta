# ==============================================================================
# PROSPECTA BACKEND - DOCKERFILE
# Image de production basée sur Eclipse Temurin 21 JRE Alpine (légère & sécurisée)
# ==============================================================================

FROM eclipse-temurin:21-jre-alpine

# Métadonnées
LABEL maintainer="Prospecta DevOps <devops@prospecta.sn>"
LABEL description="Prospecta Backend Spring Boot Application"

# Installation de curl pour le healthcheck et outils de diagnostic
RUN apk add --no-cache curl tzdata \
    && cp /usr/share/zoneinfo/Africa/Dakar /etc/localtime \
    && echo "Africa/Dakar" > /etc/timezone

# Création d'un utilisateur non-root pour respecter les bonnes pratiques de sécurité
RUN addgroup -S prospecta && adduser -S prospecta -G prospecta -h /app

WORKDIR /app

# Copie du binaire JAR généré par Maven
COPY target/*.jar /app/app.jar

# Attribution des permissions
RUN chown -R prospecta:prospecta /app

# Exécution en tant qu'utilisateur non privilégié
USER prospecta

# Port d'écoute interne de l'application
EXPOSE 8085

# Options JVM optimisées pour conteneurs (G1GC, gestion mémoire conteneur)
ENV JAVA_OPTS="-XX:+UseG1GC \
               -XX:+ExitOnOutOfMemoryError \
               -XX:MaxRAMPercentage=75.0 \
               -XX:InitialRAMPercentage=40.0 \
               -Duser.timezone=Africa/Dakar \
               -Dfile.encoding=UTF-8"

# Démarrage de l'application avec passage des variables d'environnement
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
