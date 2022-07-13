#!/bin/sh
set -e

#    This Source Code Form is subject to the terms of the Mozilla Public License,
#    v. 2.0. If a copy of the MPL was not distributed with this file, You can
#    obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
#    the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
#
#    Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
#    graphic logo is a trademark of OpenMRS Inc.

# This script is used to setup the development environment for the module.
# It is run once when the project is first clone/created.
# Downloads the required openmrs modules and installs them.

OPENMRS_SDK_PLUGIN="org.openmrs.maven.plugins:openmrs-sdk-maven-plugin"
MODULES_DIR="required_modules"

#Extract project artifactId & version
OMOD_NAME=$(mvn help:evaluate -Dexpression=project.artifactId | grep -e '^[^\[]')
OMOD_VERSION=$(mvn help:evaluate -Dexpression=project.version | grep -e '^[^\[]')
echo "Current version: $OMOD_NAME-$OMOD_VERSION"

installMaven() {
  # Linux/unix
  # TODO: Find a better way to do this (No mvn install needed)
  sh install-maven.sh
}

createEnvironmentVariablesFile() {
  cat <<EOF >.env
# OpenMRS core platform version.
OPENMRS_CORE_VERSION=dev

# To use an existing database, set the following variables.
OPENMRS_DB=localhost
OPENMRS_DB_NAME=openmrs
OPENMRS_DB_USER=openmrs
OPENMRS_DB_PASSWORD=openmrs

# To use an existing database, set this variable to 0
# To create a new database, set this variable to 1
OPENMRS_DB_REPLICAS=1

# OMOD file name
OMOD_TARGET="$OMOD_NAME-$OMOD_VERSION.omod"
EOF
}

setupOpenmrsSDK() {
  # Setup SDK
  mvn ${OPENMRS_SDK_PLUGIN}:setup-sdk -DbatchAnswers=n -B
  #docker run -it -v maven-repo:/root/.m2 maven mvn ${OPENMRS_SDK_PLUGIN}:setup-sdk -DbatchAnswers=n -B
}

downloadArtifacts() {
  # Prepare the modules dir
  if [ -d "${MODULES_DIR}" ]; then
    echo "${MODULES_DIR} dir is already exists."
    # Remove contents
    rm -rf "${MODULES_DIR:?}/"*
  else
    echo "Creating ${MODULES_DIR} directory..."
    mkdir -p "${MODULES_DIR}"
  fi

  mkdir -p artifacts
  # Download modules
  #docker run -it openmrs/openmrs-core:dev-m1 mvn "$OPENMRS_SDK_PLUGIN":build-distro -Ddistro=module.properties -Ddir=artifacts -B
  mvn ${OPENMRS_SDK_PLUGIN}:build-distro -Ddistro=module.properties -Ddir=artifacts -B
  cp -r artifacts/web/modules/* "${MODULES_DIR}"
  # Clean up artifacts
  rm -rf artifacts
}

if [ -x "$(command -v docker)" ]; then
  installed_docker_version=$(docker --version)
  echo "Installed ${installed_docker_version}"
  echo "configuring openmrs sdk..."

  # docker run openmrs/openmrs-core:dev
  # docker run openmrs/openmrs-core:dev mvn

  if ! command -v mvn -v &>/dev/null; then
    echo "Installing maven..."
    installMaven
  fi

  setupOpenmrsSDK
  downloadArtifacts
  createEnvironmentVariablesFile
else
  printf "Please install Docker and re-run prepare script.\n"
fi
