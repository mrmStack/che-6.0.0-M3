#!/usr/bin/env bats
# Copyright (c) 2017 Red Hat, Inc.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#   Roman Iuvshyn

load '/bats-support/load.bash'
load '/bats-assert/load.bash'
source /dockerfiles/cli/tests/test_base.sh

@test "test CLI 'info' command" {
  #GIVEN
  tmp_path="${TESTRUN_DIR}"/cli_cmd_info
  expected_output_1="CLI:"
  expected_output_2="Mounts:"
  expected_output_3="System:"
  expected_output_4="Internal:"
  expected_output_5="Image Registry:"

  #WHEN
  result=$(execute_cli_command --che-data-path=${tmp_path} --che-cli-command=info --che-cli-extra-options="--skip:nightly --skip:pull")

  #THEN
  [[ $result == *${expected_output_1}* ]]
  [[ $result == *${expected_output_2}* ]]
  [[ $result == *${expected_output_3}* ]]
  [[ $result == *${expected_output_4}* ]]
  [[ $result == *${expected_output_5}* ]]
}
