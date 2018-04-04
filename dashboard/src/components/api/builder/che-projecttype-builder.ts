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

/**
 * This class is providing a builder for ProjectTemplate
 * @author Florent Benoit
 */
export class CheProjectTypeBuilder {

  private type: any;

  constructor() {
    this.type = {};
    this.type.attributeDescriptors = [];

  }

  withAttributeDescriptors(attributeDescriptors: any): CheProjectTypeBuilder {
    this.type.attributeDescriptors = attributeDescriptors;
    return this;
  }

  withDisplayname(name: string): CheProjectTypeBuilder {
  this.type.displayName = name;
    return this;
  }

  withId(id: string): CheProjectTypeBuilder {
    this.type.id = id;
    return this;
  }

  build(): any {
    return this.type;
  }


}
