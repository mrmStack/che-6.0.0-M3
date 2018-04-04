#!/bin/bash
# Copyright (c) 2012-2017 Red Hat, Inc.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#   Tyler Jewell - Initial Implementation
#

pre_cmd_arun() {
  if [ ! -d /archetype/$ASSEMBLY_ID ]; then
    error "Assembly at ${ARCHETYPE_MOUNT}/$ASSEMBLY_ID not found."
    return 2
  fi
}

cmd_arun() {

case $ASSEMBLY_TYPE in
    codenvy )      CLI_IMAGE=codenvy/cli:nightly
                   DATA_MOUNT=$HOME/.codenvy/sample/data
                   ASSEMBLY_LOCATION=$ASSEMBLY_ID/assembly-codenvy/assembly-main/target/codenvy-onpremises-$ASSEMBLY_VERSION
                   ;;
    che )          CLI_IMAGE=eclipse/che-cli:nightly
                   DATA_MOUNT=$HOME/.che/sample/data
                   ASSEMBLY_LOCATION=$ASSEMBLY_ID/assembly-che/assembly-main/target/eclipse-che-$ASSEMBLY_VERSION/eclipse-che-$ASSEMBLY_VERSION
                   ;;
esac

  cd /archetype/$ASSEMBLY_ID

  RUN_COMMAND="docker run -it --rm --name run-che \
                   -v /var/run/docker.sock:/var/run/docker.sock \
                   -v \"${DATA_MOUNT}\":${CHE_CONTAINER_ROOT} \
                   -v \"${ARCHETYPE_MOUNT}\"/\"${ASSEMBLY_LOCATION}\":/assembly \
                      ${CLI_IMAGE} \
                          start --skip:nightly"
  log ${RUN_COMMAND}
  eval ${RUN_COMMAND}
}
