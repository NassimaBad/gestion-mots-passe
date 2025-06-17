# =============================================================================
# Dockerfile pour Application Java - Gestionnaire de Mots de Passe
# =============================================================================

# Image de base Java OpenJDK 17 (version slim pour réduire la taille)
FROM openjdk:17-slim-buster
# Métadonnées de l'image (bonnes pratiques)
LABEL maintainer="Nassima Badraoui et Chaimaa Bennaouar"
LABEL version="1.0"
LABEL description="Application de gestion de mots de passe avec Java RMI"
LABEL project="Cybersécurité 1A - Projet 2"

# Variables d'environnement
ENV APP_HOME=/app
ENV APP_USER=appuser
ENV RMI_PORT=1099
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Création d'un utilisateur non-root pour la sécurité
RUN groupadd -r $APP_USER && \
    useradd -r -g $APP_USER -d $APP_HOME -s /sbin/nologin $APP_USER

# Installation des dépendances système nécessaires
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    curl \
    netcat \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get clean

# Création du répertoire de l'application
RUN mkdir -p $APP_HOME/data $APP_HOME/logs $APP_HOME/config

# Définition du répertoire de travail
WORKDIR $APP_HOME

# Copie du JAR de l'application (ajustez le nom selon votre JAR)
COPY target/password-manager-1.0-SNAPSHOT.jar app.jar

# Copie des fichiers de configuration si nécessaires
# COPY docker/config/* ./config/ 2>/dev/null || :

# Attribution des permissions au répertoire
RUN chown -R $APP_USER:$APP_USER $APP_HOME && \
    chmod +x app.jar

# Création d'un script de démarrage avec vérifications
RUN echo '#!/bin/bash\n\
echo "=== Démarrage du Serveur RMI du Gestionnaire de Mots de Passe ==="\n\
echo "Utilisateur: $(whoami)"\n\
echo "Répertoire: $(pwd)"\n\
echo "Java version: $(java -version 2>&1 | head -n 1)"\n\
echo "Mémoire disponible: $(free -h | grep Mem:)"\n\
echo "Port RMI configuré: $RMI_PORT"\n\
echo "Options Java: $JAVA_OPTS"\n\
echo "====================================="\n\
\n\
# Vérification que le JAR existe\n\
if [ ! -f app.jar ]; then\n\
    echo "ERREUR: app.jar non trouvé !"\n\
    exit 1\n\
fi\n\
\n\
# Démarrage du serveur RMI\n\
exec java $JAVA_OPTS -cp app.jar com.passwordmanager.server.PasswordManagerServer' > /app/start.sh && \
    chmod +x /app/start.sh

# Basculement vers l'utilisateur non-root
USER $APP_USER

# Exposition du port RMI
EXPOSE $RMI_PORT

# Point de santé pour Docker (health check) sur le port RMI
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD nc -z localhost $RMI_PORT || exit 1

# Volumes pour la persistance des données
VOLUME ["$APP_HOME/data", "$APP_HOME/logs"]

# Point d'entrée de l'application
ENTRYPOINT ["/app/start.sh"]