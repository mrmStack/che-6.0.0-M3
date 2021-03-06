/*
 * Copyright (c) 2012-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.selenium.refactor.types;

import com.google.inject.Inject;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Paths;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.selenium.core.client.TestProjectServiceClient;
import org.eclipse.che.selenium.core.project.ProjectTemplates;
import org.eclipse.che.selenium.core.user.TestUser;
import org.eclipse.che.selenium.core.workspace.TestWorkspace;
import org.eclipse.che.selenium.pageobject.AskDialog;
import org.eclipse.che.selenium.pageobject.CodenvyEditor;
import org.eclipse.che.selenium.pageobject.Consoles;
import org.eclipse.che.selenium.pageobject.Ide;
import org.eclipse.che.selenium.pageobject.Loader;
import org.eclipse.che.selenium.pageobject.NotificationsPopupPanel;
import org.eclipse.che.selenium.pageobject.ProjectExplorer;
import org.eclipse.che.selenium.pageobject.Refactor;
import org.eclipse.che.selenium.refactor.Services;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** @author Musienko Maxim */
public class TestFailTest {
  private static final Logger LOG = LoggerFactory.getLogger(TestEnumerationsTest.class);
  private static final String nameOfProject =
      NameGenerator.generate(TestFailTest.class.getSimpleName(), 3);
  private static final String pathToPackageInChePrefix =
      nameOfProject + "/src" + "/main" + "/java" + "/renametype";
  private String renameItem = "B.java";
  private String pathToCurrentPackage;
  private Services services;

  @Inject private TestWorkspace workspace;
  @Inject private TestUser defaultTestUser;
  @Inject private Ide ide;
  @Inject private ProjectExplorer projectExplorer;
  @Inject private Loader loader;
  @Inject private CodenvyEditor editor;
  @Inject private Refactor refactorPanel;
  @Inject private Consoles consoles;
  @Inject private AskDialog askDialog;
  @Inject private NotificationsPopupPanel notificationsPopupPanel;
  @Inject private Refactor refactor;
  @Inject private TestProjectServiceClient testProjectServiceClient;

  @BeforeClass
  public void setup() throws Exception {
    services = new Services(projectExplorer, notificationsPopupPanel, refactor);

    URL resource = TestFailTest.this.getClass().getResource("/projects/RenameType");
    testProjectServiceClient.importProject(
        workspace.getId(),
        Paths.get(resource.toURI()),
        nameOfProject,
        ProjectTemplates.MAVEN_SIMPLE);

    ide.open(workspace);
    projectExplorer.waitVisibleItem(nameOfProject);
    consoles.closeProcessesArea();
    projectExplorer.quickExpandWithJavaScript();
    loader.waitOnClosed();
  }

  @BeforeMethod
  public void setFieldsForTest(Method method) {
    try {
      String nameCurrentTest = method.getName();
      pathToCurrentPackage = pathToPackageInChePrefix + "/" + nameCurrentTest;
    } catch (Exception e) {
      LOG.error(e.getLocalizedMessage(), e);
    }
  }

  @Test
  public void testFail26() {
    loader.waitOnClosed();
    projectExplorer.openItemByPath(pathToCurrentPackage + "/A.java");
    editor.waitActive();
    services.invokeRefactorWizardForProjectExplorerItem(pathToCurrentPackage + "/A.java");
    doRefactorWithWifget(renameItem);
    refactorPanel.waitTextInErrorMessage("Compilation unit 'B.java' already exists");
    refactorPanel.clickCancelButtonRefactorForm();
  }

  @Test(priority = 1)
  public void testFail35() {
    projectExplorer.openItemByPath(pathToCurrentPackage + "/A.java");
    editor.waitActive();
    services.invokeRefactorWizardForProjectExplorerItem(pathToCurrentPackage + "/A.java");
    doRefactorWithWifget(renameItem);
    askDialog.waitFormToOpen();
    askDialog.acceptDialogWithText(
        "Found potential matches. Please review changes on the preview page.");
    askDialog.waitFormToClose();
  }

  @Test(priority = 2)
  public void testFail80() {
    projectExplorer.openItemByPath(pathToCurrentPackage + "/A.java");
    editor.waitActive();
    services.invokeRefactorWizardForProjectExplorerItem(pathToCurrentPackage + "/A.java");
    doRefactorWithWifget(renameItem);
    askDialog.waitFormToOpen();
    askDialog.acceptDialogWithText(
        "Local Type declared inside 'renametype.testFail80.A' is named B");
    askDialog.waitFormToClose();
  }

  /**
   * type new class into field of the refactoring widget and clickOkBtn
   *
   * @param newClassName the new class for refactoring
   */
  private void doRefactorWithWifget(String newClassName) {
    try {
      refactorPanel.typeNewName(newClassName);
      refactorPanel.clickOkButtonRefactorForm();
    } catch (WebDriverException ex) {
      LOG.warn(ex.getLocalizedMessage());
      refactorPanel.typeNewName(newClassName);
      refactorPanel.sendKeysIntoField(Keys.ARROW_LEFT.toString());
      refactorPanel.sendKeysIntoField(Keys.ARROW_LEFT.toString());
      refactorPanel.clickOkButtonRefactorForm();
    }
  }
}
