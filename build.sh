#!/bin/bash

# Script to build the personal-assistant Spring Boot project
# Usage: ./build.sh

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Load environment variables from .env file
ENV_FILE=".env"
if [ -f "$ENV_FILE" ]; then
    echo -e "${GREEN}Loading environment variables from $ENV_FILE${NC}"
    set -a  # Automatically export all variables
    source "$ENV_FILE"
    set +a  # Stop automatically exporting
else
    echo -e "${YELLOW}Warning: $ENV_FILE not found${NC}"
    echo -e "${YELLOW}Environment variables will use defaults or system environment${NC}"
    echo -e "${YELLOW}To create it: cp .env.example .env${NC}"
fi

# Set JVM path (from .env or use default)
if [ -z "$JAVA_HOME" ]; then
    export JAVA_HOME=/Users/damon/IdeaProjects/CRD/jdk-21.0.9/Contents/Home
    echo -e "${YELLOW}Using default JAVA_HOME: $JAVA_HOME${NC}"
fi
export PATH=$JAVA_HOME/bin:$PATH

# Set Maven path (from .env or use default)
if [ -z "$MAVEN_HOME" ]; then
    export MAVEN_HOME=/Users/damon/IdeaProjects/apache-maven-3.9.11
    echo -e "${YELLOW}Using default MAVEN_HOME: $MAVEN_HOME${NC}"
fi
export PATH=$MAVEN_HOME/bin:$PATH

echo -e "${GREEN}Building personal-assistant...${NC}"
echo ""
echo -e "${YELLOW}Using JVM: $JAVA_HOME${NC}"
echo -e "${YELLOW}Using Maven: $MAVEN_HOME${NC}"
echo ""

mvn clean package

echo ""
echo -e "${GREEN}Build completed successfully.${NC}"
echo -e "${YELLOW}Run with: java -jar target/personal-assistant-*.jar${NC}"
echo -e "${YELLOW}Or:       mvn spring-boot:run${NC}"
