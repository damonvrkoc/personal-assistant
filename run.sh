#!/bin/bash

# Script to run the personal-assistant agent (Spring Boot)
# Usage: ./run.sh
# Build first if needed: ./build.sh

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
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

# JAR file (built by ./build.sh)
JAR_FILE="target/personal-assistant-0.1.0-SNAPSHOT.jar"

# Fall back to any matching JAR if version changes
if [ ! -f "$JAR_FILE" ]; then
    JAR_MATCH=(target/personal-assistant-*.jar)
    if [ -f "${JAR_MATCH[0]}" ]; then
        JAR_FILE="${JAR_MATCH[0]}"
    fi
fi

if [ ! -f "$JAR_FILE" ]; then
    echo -e "${RED}Error: JAR file not found in target/${NC}"
    echo -e "${YELLOW}Please build the project first using: ./build.sh${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}Starting personal-assistant agent...${NC}"
echo ""
echo -e "${YELLOW}Using JVM: $JAVA_HOME${NC}"
echo -e "${BLUE}JAR file: $JAR_FILE${NC}"
if [ -n "$SPRING_PROFILES_ACTIVE" ]; then
    echo -e "${BLUE}Spring profile: $SPRING_PROFILES_ACTIVE${NC}"
fi
echo ""

java -jar "$JAR_FILE"

echo ""
echo -e "${GREEN}Agent stopped${NC}"
