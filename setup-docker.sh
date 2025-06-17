#!/bin/bash
# =============================================================================
# Setup Script - Projet Docker Gestionnaire de Mots de Passe
# Usage: ./setup-docker.sh
# =============================================================================

set -e

# Couleurs
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }

echo "🐳 Configuration du Projet Docker - Gestionnaire de Mots de Passe"
echo "=================================================================="

# Création de la structure des dossiers
log_info "Création de la structure des dossiers..."

# Dossiers principaux
mkdir -p docker/scripts
mkdir -p docker/config
mkdir -p docker/docs
mkdir -p data
mkdir -p logs
mkdir -p config

# Permissions pour les scripts
chmod +x docker/scripts/*.sh 2>/dev/null || true

log_success "Structure de dossiers créée"

# Vérification des prérequis
log_info "Vérification des prérequis..."

# Docker
if command -v docker &> /dev/null; then
    DOCKER_VERSION=$(docker --version | cut -d' ' -f3 | cut -d',' -f1)
    log_success "Docker installé: $DOCKER_VERSION"
else
    log_warning "Docker non installé - Téléchargez depuis https://docker.com"
fi

# Maven
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn --version | head -n1 | cut -d' ' -f3)
    log_success "Maven installé: $MVN_VERSION"
else
    log_warning "Maven non installé"
fi

# Java
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2)
    log_success "Java installé: $JAVA_VERSION"
else
    log_warning "Java non installé"
fi

# Création des fichiers de configuration par défaut
log_info "Création des fichiers de configuration..."

# .gitignore pour Docker
cat > .gitignore.docker << 'EOF'
# Docker
.dockerignore
docker/data/
docker/logs/
*.log

# Temporary files
*.tmp
*.temp
EOF

if [ ! -f .gitignore ]; then
    mv .gitignore.docker .gitignore
    log_success "Fichier .gitignore créé"
else
    cat .gitignore.docker >> .gitignore
    rm .gitignore.docker
    log_success "Fichier .gitignore mis à jour"
fi

# Configuration par défaut pour l'application
cat > config/application.properties << 'EOF'
# Configuration de l'application
server.port=8080
app.name=Gestionnaire de Mots de Passe
app.version=1.0
app.environment=docker

# Logs
logging.level.root=INFO
logging.file.name=/app/logs/application.log
EOF

log_success "Configuration par défaut créée"

# Affichage des prochaines étapes
echo ""
echo "🎯 Prochaines étapes:"
echo "1. Compilez votre application Java:"
echo "   mvn clean install"
echo ""
echo "2. Construisez l'image Docker:"
echo "   docker build -t password-manager ."
echo ""
echo "3. Démarrez le conteneur Docker (serveur RMI et client JavaFX):"
echo "   docker-compose up --build -d"
echo ""
echo "4. Testez l'installation en accédant à l'application JavaFX."
echo ""

log_success "Setup terminé avec succès!"
echo "📖 Consultez le README.md pour la documentation complète"