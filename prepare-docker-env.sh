#!/bin/sh
set -e

#    This Source Code Form is subject to the terms of the Mozilla Public License,
#    v. 2.0. If a copy of the MPL was not distributed with this file, You can
#    obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
#    the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
#
#    Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
#    graphic logo is a trademark of OpenMRS Inc.

# This script is used to setup the development environment for this module.
# It should be run once when the project is first clone/created.
# Downloads the required openmrs modules and installs them.

OPENMRS_SDK_PLUGIN="org.openmrs.maven.plugins:openmrs-sdk-maven-plugin"
MODULES_DIR="required_modules"

# Extract project artifactId & version
OMOD_NAME=$(mvn help:evaluate -Dexpression=project.artifactId | grep -e '^[^\[]')
OMOD_VERSION=$(mvn help:evaluate -Dexpression=project.version | grep -e '^[^\[]')
echo "Current version: $OMOD_NAME-$OMOD_VERSION"

create_environment_variables_file() {
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

prepare_modules_directory() {
    # prepare modules directory
    if [ -d "${MODULES_DIR}" ]; then
      echo "${MODULES_DIR} dir is already exists."
      # remove contents
      rm -rf "${MODULES_DIR:?}/"*
    else
      echo "Creating ${MODULES_DIR} directory..."
      mkdir -p "${MODULES_DIR}"
    fi
}

download_artifacts() {
  mkdir -p artifacts
  # download modules
  docker run --rm -w="/module" -v ${PWD}:/module openmrs/openmrs-core:dev mvn ${OPENMRS_SDK_PLUGIN}:build-distro -Ddistro=module.properties -Ddir=artifacts -B
  # copy downloaded modules to ${MODULES_DIR} directory
  cp -r artifacts/web/modules/* "${MODULES_DIR}"
  # clean up artifacts
  rm -rf artifacts
}

if [ -x "$(command -v docker)" ]; then
  installed_docker_version=$(docker --version)
  echo "Installed ${installed_docker_version}"

  prepare_modules_directory
  download_artifacts
  create_environment_variables_file
else
  printf "Please install Docker and re-run prepare script.\n"
fi
