/*
 * Copyright (c) 2015-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
'use strict';

import {CheEnvironmentRegistry} from '../../../components/api/environment/che-environment-registry.factory';
import {EnvironmentManager} from '../../../components/api/environment/environment-manager';
import {IEnvironmentManagerMachine} from '../../../components/api/environment/environment-manager-machine';
import {CreateWorkspaceSvc} from './create-workspace.service';
import {NamespaceSelectorSvc} from './namespace-selector/namespace-selector.service';
import {StackSelectorSvc} from './stack-selector/stack-selector.service';
import {RandomSvc} from '../../../components/utils/random.service';

/**
 * This class is handling the controller for workspace creation.
 *
 * @author Oleksii Kurinnyi
 */
export class CreateWorkspaceController {
  /**
   * Timeout service.
   */
  private $timeout: ng.ITimeoutService;
  /**
   * The registry of environment managers.
   */
  private cheEnvironmentRegistry: CheEnvironmentRegistry;
  /**
   * Workspace creation service.
   */
  private createWorkspaceSvc: CreateWorkspaceSvc;
  /**
   * Namespace selector service.
   */
  private namespaceSelectorSvc: NamespaceSelectorSvc;
  /**
   * Stack selector service.
   */
  private stackSelectorSvc: StackSelectorSvc;
  /**
   * Generator for random strings.
   */
  private randomSvc: RandomSvc;
  /**
   * The environment manager.
   */
  private environmentManager: EnvironmentManager;
  /**
   * The selected stack.
   */
  private stack: che.IStack;
  /**
   * The selected namespace ID.
   */
  private namespaceId: string;
  /**
   * The list of machines of selected stack.
   */
  private stackMachines: Array<IEnvironmentManagerMachine>;
  /**
   * Desired memory limit by machine name.
   */
  private memoryByMachine: {[name: string]: number};
  /**
   * The map of forms.
   */
  private forms: Map<string, ng.IFormController>;
  /**
   * The list of names of existing workspaces.
   */
  private usedNamesList: string[];
  /**
   * The name of workspace.
   */
  private workspaceName: string;
  /**
   * Hide progress loader if <code>true</code>.
   */
  private hideLoader: boolean;

  /**
   * Default constructor that is using resource injection
   * @ngInject for Dependency injection
   */
  constructor($timeout: ng.ITimeoutService, cheEnvironmentRegistry: CheEnvironmentRegistry, createWorkspaceSvc: CreateWorkspaceSvc, namespaceSelectorSvc: NamespaceSelectorSvc, stackSelectorSvc: StackSelectorSvc, randomSvc: RandomSvc) {
    this.$timeout = $timeout;
    this.cheEnvironmentRegistry = cheEnvironmentRegistry;
    this.createWorkspaceSvc = createWorkspaceSvc;
    this.namespaceSelectorSvc = namespaceSelectorSvc;
    this.stackSelectorSvc = stackSelectorSvc;
    this.randomSvc = randomSvc;

    this.usedNamesList = [];
    this.stackMachines = [];
    this.memoryByMachine = {};
    this.forms = new Map();

    this.namespaceId = this.namespaceSelectorSvc.getNamespaceId();
    this.buildListOfUsedNames().then(() => {
      this.workspaceName = this.randomSvc.getRandString({prefix: 'wksp-', list: this.usedNamesList});
      this.reValidateName();
    });

    // loader should be hidden and page content shown
    // when stacks selector is rendered
    // and default stack is selected
    this.hideLoader = false;
  }

  /**
   * Callback which is called when stack is selected.
   *
   * @param {string} stackId the stack ID
   */
  onStackSelected(stackId: string): void {
    // tiny timeout for templates selector to be rendered
    this.$timeout(() => {
      this.hideLoader = true;
    }, 10);

    this.stack = this.stackSelectorSvc.getStackById(stackId);

    if (!this.stack.workspaceConfig) {
      this.memoryByMachine = {};
      this.stackMachines = [];
      return;
    }

    const environmentName = this.stack.workspaceConfig.defaultEnv;
    const environment = this.stack.workspaceConfig.environments[environmentName];
    const recipeType = environment.recipe.type;
    this.environmentManager = this.cheEnvironmentRegistry.getEnvironmentManager(recipeType);

    this.memoryByMachine = {};
    this.stackMachines = this.environmentManager.getMachines(environment);
  }

  /**
   * Callback which is called when machine's memory limit is changes.
   *
   * @param {string} name a machine name
   * @param {number} memoryLimitBytes a machine's memory limit in bytes
   */
  onRamChanged(name: string, memoryLimitBytes: number): void {
    this.memoryByMachine[name] = memoryLimitBytes;
  }

  /**
   * Callback which is called when namespace is selected.
   *
   * @param {string} namespaceId a namespace ID
   */
  onNamespaceChanged(namespaceId: string) {
    this.namespaceId = namespaceId;

    this.buildListOfUsedNames().then(() => {
      this.reValidateName();
    });
  }

  /**
   * Returns list of namespaces.
   *
   * @return {Array<che.INamespace>}
   */
  getNamespaces(): Array<che.INamespace> {
    return this.namespaceSelectorSvc.getNamespaces();
  }

  /**
   * Returns namespaces empty message if set.
   *
   * @returns {string}
   */
  getNamespaceEmptyMessage(): string {
    return this.namespaceSelectorSvc.getNamespaceEmptyMessage();
  }

  /**
   * Returns namespaces caption.
   *
   * @returns {string}
   */
  getNamespaceCaption(): string {
    return this.namespaceSelectorSvc.getNamespaceCaption();
  }

  /**
   * Returns <code>true</code> when 'Create' button should be disabled.
   *
   * @return {boolean}
   */
  isCreateButtonDisabled(): boolean {
    if (!this.namespaceId || (this.stack && !this.stack.workspaceConfig)) {
      return true;
    }

    for (const form of this.forms.values()) {
      if (form.$valid !== true) {
        return true;
      }
    }

    return false;
  }

  /**
   * Stores forms in list.
   *
   * @param {string} inputName
   * @param {ng.IFormController} form
   */
  registerForm(inputName: string, form: ng.IFormController) {
    this.forms.set(inputName, form);
  }

  /**
   * Returns <code>false</code> if workspace's name is not unique in the namespace.
   * Only member with 'manageWorkspaces' permission can definitely know whether
   * name is unique or not.
   *
   * @param {string} name workspace's name
   */
  isNameUnique(name: string): boolean {
    return this.usedNamesList.indexOf(name) === -1;
  }

  /**
   * Filters list of workspaces by current namespace and
   * builds list of names for current namespace.
   *
   * @return {IPromise<any>}
   */
  buildListOfUsedNames(): ng.IPromise<any> {
    return this.createWorkspaceSvc.fetchWorkspacesByNamespace(this.namespaceId).then((workspaces: Array<che.IWorkspace>) => {
      this.usedNamesList = workspaces.filter((workspace: che.IWorkspace) => {
        return workspace.namespace === this.namespaceId;
      }).map((workspace: che.IWorkspace) => {
        return workspace.config.name;
      });
    });
  }

  /**
   * Triggers form validation on Settings tab.
   */
  reValidateName(): void {
    const form: ng.IFormController = this.forms.get('name');

    if (!form) {
      return;
    }

    ['name', 'deskname'].forEach((inputName: string) => {
      const model = form[inputName] as ng.INgModelController;
      if (model) {
        model.$validate();
      }
    });
  }

  /**
   * Creates workspace.
   */
  createWorkspace(): void {
    // update workspace name
    this.stack.workspaceConfig.name = this.workspaceName;

    // update memory limits of machines
    if (Object.keys(this.memoryByMachine).length !== 0) {
      this.stackMachines.forEach((machine: IEnvironmentManagerMachine) => {
        if (this.memoryByMachine[machine.name]) {
          this.environmentManager.setMemoryLimit(machine, this.memoryByMachine[machine.name]);
        }
      });
      const environmentName = this.stack.workspaceConfig.defaultEnv;
      const environment = this.stack.workspaceConfig.environments[environmentName];
      const newEnvironment = this.environmentManager.getEnvironment(environment, this.stackMachines);
      this.stack.workspaceConfig.environments[environmentName] = newEnvironment;
    }
    let attributes = {stackId: this.stack.id};
    let workspaceConfig = angular.copy(this.stack.workspaceConfig);
    this.createWorkspaceSvc.createWorkspace(workspaceConfig, attributes);
  }

}
