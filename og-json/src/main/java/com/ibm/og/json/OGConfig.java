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
  public OperationConfig listObjectVersions;
  public OperationConfig containerList;
  public OperationConfig containerCreate;
  public OperationConfig multipartWrite;
  public OperationConfig multiDelete;
  public OperationConfig writeCopy;
  public OperationConfig writeLegalhold;
  public OperationConfig deleteLegalhold;
  public OperationConfig readLegalhold;
  public OperationConfig extendRetention;
  public OperationConfig putObjectLockRetention;
  public OperationConfig getObjectLockRetention;
  public OperationConfig putObjectLockLegalHold;
  public OperationConfig getObjectLockLegalHold;
  public OperationConfig objectRestore;
  public OperationConfig putContainerLifecycle;
  public OperationConfig getContainerLifecycle;
  public OperationConfig deleteContainerLifecycle;
  public OperationConfig putContainerProtection;
  public OperationConfig getContainerProtection;
  public OperationConfig writeTags;
  public OperationConfig deleteTags;
  public OperationConfig getTags;

  public OperationConfig writeSelectObject;

  public SelectionConfig<FilesizeConfig> filesize;
  public DataType data;
  public ConcurrencyConfig concurrency;
  public AuthenticationConfig authentication;
  public ClientConfig client;
  public StoppingConditionsConfig stoppingConditions;
  public FailingConditionsConfig failingConditions;
  public ObjectManagerConfig objectManager;
  public boolean shutdownImmediate;
  public boolean abortMpuWhenStopping;
  public int shutdownTimeout;
  public boolean virtualHost;
  public Integer statsLogInterval;
  public boolean octalNamingMode;

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
    this.listObjectVersions = new OperationConfig();
    this.containerList = new OperationConfig();
    this.containerCreate = new OperationConfig();
    this.multipartWrite = new OperationConfig();
    this.multiDelete = new OperationConfig();
    this.writeCopy = new OperationConfig();
    this.writeLegalhold = new OperationConfig();
    this.deleteLegalhold = new OperationConfig();
    this.readLegalhold = new OperationConfig();
    this.extendRetention = new OperationConfig();
    this.objectRestore = new OperationConfig();
    this.putContainerLifecycle = new OperationConfig();
    this.getContainerLifecycle = new OperationConfig();
    this.deleteContainerLifecycle = new OperationConfig();
    this.putContainerProtection = new OperationConfig();
    this.getContainerProtection = new OperationConfig();
    this.putObjectLockLegalHold = new OperationConfig();
    this.getObjectLockLegalHold = new OperationConfig();
    this.putObjectLockRetention = new OperationConfig();
    this.getObjectLockRetention = new OperationConfig();
    this.writeTags = new OperationConfig();
    this.deleteTags = new OperationConfig();
    this.getTags = new OperationConfig();
    this.writeSelectObject = new OperationConfig();
    this.filesize = null;
    this.data = DataType.RANDOM;
    this.concurrency = null;
    this.authentication = new AuthenticationConfig();
    this.client = new ClientConfig();
    this.stoppingConditions = new StoppingConditionsConfig();
    this.failingConditions = new FailingConditionsConfig();
    this.objectManager = new ObjectManagerConfig();
    this.abortMpuWhenStopping = false;
    this.shutdownImmediate = true;
    this.shutdownTimeout = 3600;
    this.virtualHost = false;
    this.statsLogInterval = -1; //seconds
    this.octalNamingMode = false;

  }
}
