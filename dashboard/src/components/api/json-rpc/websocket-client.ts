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
import {ICommunicationClient} from './json-rpc-client';

/**
 * The implementation for JSON RPC protocol communication through websocket.
 *
 * @author Ann Shumilova
 */
export class WebsocketClient implements ICommunicationClient {
  onResponse: Function;
  private $websocket: any;
  private $q: ng.IQService;
  private websocketStream;

  constructor ($websocket: any, $q: ng.IQService) {
    this.$websocket = $websocket;
    this.$q = $q;
  }

  /**
   * Performs connection to the pointed entrypoint.
   *
   * @param entrypoint the entrypoint to connect to
   */
  connect(entrypoint: string): ng.IPromise<any> {
    let deferred = this.$q.defer();
    this.websocketStream = this.$websocket(entrypoint);
    this.websocketStream.onOpen(() => {
      deferred.resolve();
    });

    this.websocketStream.onError(() => {
      deferred.reject();
    });
    this.websocketStream.onMessage((message: any) => {
      let data = JSON.parse(message.data);
      this.onResponse(data);
    });

    return deferred.promise;
  }

  /**
   * Performs closing the connection.
   */
  disconnect(): void {
    if (this.websocketStream) {
      this.websocketStream.close();
    }
  }

  /**
   * Sends pointed data.
   *
   * @param data to be sent
   */
  send(data: any): void {
    this.websocketStream.send(data);
  }

  /**
   * Provides defered object.
   *
   * @returns {IDeferred<T>}
   */
  getDeferred(): ng.IDeferred<any> {
    return this.$q.defer();
  }
}
