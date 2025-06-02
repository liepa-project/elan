#!/bin/bash

# This script builds the Java application and then creates a self-contained
# executable using jpackage.

# --- Configuration ---
# Set the main class of your application
MAIN_CLASS="mpi.eudico.client.annotator.ELAN"
# Set the desired name for your packaged application
APP_NAME="ELAN"
# Set the vendor name
VENDOR="Šnekanti Liepa"
# Set the application description
DESCRIPTION="ELAN sulietuvinimas"

# --- Paths ---
# Get the directory where the script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
# Maven build directory
BUILD_DIR="${SCRIPT_DIR}/target"
# Name of the final JAR file (e.g., hello-window-1.0-SNAPSHOT.jar)
# This assumes the artifactId and version from your pom.xml
JAR_FILE="${BUILD_DIR}/elan-6.9.jar"
# Output directory for the jpackage application
JPACKAGE_OUTPUT_DIR="${BUILD_DIR}/jpackage-output"
JPACKAGE_INPUT_DIR="${BUILD_DIR}/jpackage-input"

# --- Pre-requisites Check ---
# Check if JAVA_HOME is set
if [ -z "${JAVA_HOME}" ]; then
  echo "Error: JAVA_HOME environment variable is not set."
  echo "Please set JAVA_HOME to your JDK 17 (or newer) installation directory."
  exit 1
fi

# Check if jpackage executable exists
JPACKAGE_EXE="${JAVA_HOME}/bin/jpackage"
if [ ! -f "${JPACKAGE_EXE}" ]; then
  echo "Error: jpackage executable not found at ${JPACKAGE_EXE}."
  echo "Please ensure JAVA_HOME points to a valid JDK 17+ installation."
  exit 1
fi

# --- Build the Java application with Maven ---
echo "Building the Java application with Maven..."
#mvn clean package
mvn clean package -Dexec.phase=install

# Check if Maven build was successful
if [ $? -ne 0 ]; then
  echo "Maven build failed. Exiting."
  exit 1
fi

# Check if the JAR file was created
if [ ! -f "${JAR_FILE}" ]; then
  echo "Error: Application JAR file not found at ${JAR_FILE}."
  echo "Please check your Maven build output."
  exit 1
fi

mkdir -p ${JPACKAGE_INPUT_DIR}
cp ${JAR_FILE} ${JPACKAGE_INPUT_DIR}
cp -r ${BUILD_DIR}/lib ${JPACKAGE_INPUT_DIR}/lib


# --- Create self-contained executable with jpackage ---
echo "Creating self-contained executable with jpackage..."

# Create the output directory if it doesn't exist
mkdir -p "${JPACKAGE_OUTPUT_DIR}"


"${JPACKAGE_EXE}" \
  --input "${JPACKAGE_INPUT_DIR}" \
  --name "${APP_NAME}" \
  --main-jar "$(basename "${JAR_FILE}")" \
  --main-class "${MAIN_CLASS}" \
  --type app-image \
  --vendor "${VENDOR}" \
  --description "${DESCRIPTION}" \
  --dest "${JPACKAGE_OUTPUT_DIR}-linux" \
  --module-path "${JAVA_HOME}/jmods" \
  --add-modules java.desktop,java.logging \
  --verbose # Add verbose output for debugging

echo "jpackage process completed."
echo "Your self-contained application should be in: ${JPACKAGE_OUTPUT_DIR}"

