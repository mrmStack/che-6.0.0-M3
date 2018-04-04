/**
 * ***************************************************************************** Copyright (c) 2016
 * Rogue Wave Software, Inc. All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: Rogue Wave Software, Inc. - initial API and implementation
 * *****************************************************************************
 */
package org.eclipse.che.plugin.testing.phpunit.server;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import org.eclipse.che.api.testing.server.framework.TestRunner;
import org.eclipse.che.api.testing.shared.TestDetectionContext;
import org.eclipse.che.api.testing.shared.TestExecutionContext;
import org.eclipse.che.api.testing.shared.TestPosition;
import org.eclipse.che.commons.lang.execution.ProcessHandler;
import org.eclipse.che.dto.server.DtoFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PHPUnit implementation for the test runner service.
 *
 * <p><em>projectPath</em> : Relative path to the project directory <em>absoluteProjectPath</em> :
 * Absolute path to the project directory <em>testTarget</em> : Path to test target (container or
 * script). </pre>
 *
 * @author Bartlomiej Laczkowski
 */
public class PHPUnitTestRunner implements TestRunner {

  public static final Logger LOG = LoggerFactory.getLogger(PHPUnitTestRunner.class);
  public static final String RUNNER_ID = "PHPUnit";

  private final PHPUnitTestEngine testEngine;

  @Inject
  public PHPUnitTestRunner(@Named("che.user.workspaces.storage") File projectsRoot) {
    testEngine = new PHPUnitTestEngine(projectsRoot);
  }

  /** {@inheritDoc} */
  @Override
  public String getName() {
    return RUNNER_ID;
  }

  @Override
  public int getDebugPort() {
    return -1;
  }

  @Override
  public List<TestPosition> detectTests(TestDetectionContext context) {
    List<TestPosition> result = new ArrayList<>();

    String filePath = context.getFilePath();
    if (filePath.endsWith(".php") || filePath.endsWith(".phtml")) {
      TestPosition testPosition =
          DtoFactory.newDto(TestPosition.class).withFrameworkName(getName());
      result.add(testPosition);
    }
    return result;
  }

  @Override
  public ProcessHandler execute(TestExecutionContext context) {
    return testEngine.executeTests(context);
  }
}
