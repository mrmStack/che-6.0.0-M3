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
package org.eclipse.che.selenium.pageobject.dashboard.workspaces;

import static org.eclipse.che.selenium.core.constant.TestTimeoutsConstants.REDRAW_UI_ELEMENTS_TIMEOUT_SEC;
import static org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOfElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.che.selenium.core.SeleniumWebDriver;
import org.eclipse.che.selenium.core.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** @author Musienko Maxim */
@Singleton
public class WorkspaceProjects {

  private final SeleniumWebDriver seleniumWebDriver;

  @Inject
  public WorkspaceProjects(SeleniumWebDriver seleniumWebDriver) {
    this.seleniumWebDriver = seleniumWebDriver;
    PageFactory.initElements(seleniumWebDriver, this);
  }

  private interface Locators {
    String PROJECT_BY_NAME = "//div[contains(@ng-click, 'projectItem')]/span[text()='%s']";
    String DELETE_PROJECT = "//button/span[text()='Delete']";
    String DELETE_IT_PROJECT = "//che-button-primary[@che-button-title='Delete']/button";
    String ADD_NEW_PROJECT_BUTTON = "//che-button-primary[@che-button-title='Add Project']/button";
  }

  /**
   * wait the project is present in the 'All projects' tab
   *
   * @param projectName name of project
   */
  public void waitProjectIsPresent(String projectName) {
    new WebDriverWait(seleniumWebDriver, REDRAW_UI_ELEMENTS_TIMEOUT_SEC)
        .until(
            visibilityOfElementLocated(
                By.xpath(String.format(Locators.PROJECT_BY_NAME, projectName))));
  }

  /**
   * wait the project is not present in the 'All projects' tab
   *
   * @param projectName name of project
   */
  public void waitProjectIsNotPresent(String projectName) {
    new WebDriverWait(seleniumWebDriver, REDRAW_UI_ELEMENTS_TIMEOUT_SEC)
        .until(
            invisibilityOfElementLocated(
                By.xpath(String.format(Locators.PROJECT_BY_NAME, projectName))));
  }

  /**
   * open settings for project by name in the 'All projects' tab
   *
   * @param projectName name of project
   */
  public void openSettingsForProjectByName(String projectName) {
    new WebDriverWait(seleniumWebDriver, REDRAW_UI_ELEMENTS_TIMEOUT_SEC)
        .until(
            ExpectedConditions.visibilityOfElementLocated(
                By.xpath(String.format(Locators.PROJECT_BY_NAME, projectName))))
        .click();
  }

  /** click on 'DELETE' button in settings of project */
  public void clickOnDeleteProject() {
    new WebDriverWait(seleniumWebDriver, REDRAW_UI_ELEMENTS_TIMEOUT_SEC)
        .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(Locators.DELETE_PROJECT)))
        .click();
  }

  /** click on 'DELETE IT!' button in the confirming window */
  public void clickOnDeleteItInDialogWindow() {
    WaitUtils.sleepQuietly(1);
    new WebDriverWait(seleniumWebDriver, REDRAW_UI_ELEMENTS_TIMEOUT_SEC)
        .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(Locators.DELETE_IT_PROJECT)))
        .click();
  }

  /** click on the Add Project button */
  public void clickOnAddNewProjectButton() {
    new WebDriverWait(seleniumWebDriver, REDRAW_UI_ELEMENTS_TIMEOUT_SEC)
        .until(visibilityOfElementLocated(By.xpath(Locators.ADD_NEW_PROJECT_BUTTON)))
        .click();
  }
}
