/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.json;

import java.util.Map;

import com.ibm.og.api.DataType;
import com.ibm.og.http.Api;
import com.ibm.og.http.Scheme;
import com.google.common.collect.Maps;

public class OGConfig {
  public Scheme scheme;
  public SelectionConfig<String> host;
  public Integer port;
  public Api api;
  public String uriRoot;
  public ContainerConfig container;
  public Map<String, SelectionConfig<String>> headers;
  public OperationConfig write;
  public OperationConfig overwrite;
  public OperationConfig metadata;
  public OperationConfig read;
  public OperationConfig delete;
  public OperationConfig list;
  public OperationConfig containerList;
  public OperationConfig containerCreate;
  public OperationConfig multipartWrite;
  public OperationConfig writeCopy;
  public OperationConfig writeLegalhold;
  public OperationConfig deleteLegalhold;
  public OperationConfig readLegalhold;
  public OperationConfig extendRetention;
  public OperationConfig objectRestore;
  public OperationConfig putContainerLifecycle;
  public OperationConfig getContainerLifecycle;
  public SelectionConfig<FilesizeConfig> filesize;
  public DataType data;
  public ConcurrencyConfig concurrency;
  public AuthenticationConfig authentication;
  public ClientConfig client;
  public StoppingConditionsConfig stoppingConditions;
  public FailingConditionsConfig failingConditions;
  public ObjectManagerConfig objectManager;
  public boolean shutdownImmediate;
  public boolean virtualHost;
  public Integer statsLogInterval;

  public OGConfig() {
    this.scheme = Scheme.HTTP;
    this.host = null;
    this.port = null;
    this.api = null;
    this.uriRoot = null;
    this.container = new ContainerConfig();
    this.headers = Maps.newLinkedHashMap();
    this.write = new OperationConfig();
    this.overwrite = new OperationConfig();
    this.read = new OperationConfig();
    this.metadata = new OperationConfig();
    this.delete = new OperationConfig();
    this.list = new OperationConfig();
    this.containerList = new OperationConfig();
    this.containerCreate = new OperationConfig();
    this.multipartWrite = new OperationConfig();
    this.writeCopy = new OperationConfig();
    this.writeLegalhold = new OperationConfig();
    this.deleteLegalhold = new OperationConfig();
    this.readLegalhold = new OperationConfig();
    this.extendRetention = new OperationConfig();
    this.objectRestore = new OperationConfig();
    this.putContainerLifecycle = new OperationConfig();
    this.getContainerLifecycle = new OperationConfig();
    this.filesize = null;
    this.data = DataType.RANDOM;
    this.concurrency = null;
    this.authentication = new AuthenticationConfig();
    this.client = new ClientConfig();
    this.stoppingConditions = new StoppingConditionsConfig();
    this.failingConditions = new FailingConditionsConfig();
    this.objectManager = new ObjectManagerConfig();
    this.shutdownImmediate = true;
    this.virtualHost = false;
    this.statsLogInterval = 300; //seconds

  }
}
