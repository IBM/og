/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.json;

import java.util.Map;

import com.cleversafe.og.api.DataType;
import com.cleversafe.og.http.Api;
import com.cleversafe.og.http.Scheme;
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
  public OperationConfig read;
  public OperationConfig delete;
  public SelectionConfig<FilesizeConfig> filesize;
  public DataType data;
  public ConcurrencyConfig concurrency;
  public AuthenticationConfig authentication;
  public ClientConfig client;
  public StoppingConditionsConfig stoppingConditions;
  public ObjectManagerConfig objectManager;
  public boolean virtualHost;

  public OGConfig() {
    this.scheme = Scheme.HTTP;
    this.host = null;
    this.port = null;
    this.api = Api.SOH;
    this.uriRoot = null;
    this.container = new ContainerConfig();
    this.headers = Maps.newLinkedHashMap();
    this.write = new OperationConfig();
    this.read = new OperationConfig();
    this.delete = new OperationConfig();
    this.filesize = new SelectionConfig<FilesizeConfig>();
    this.filesize.choices.add(new ChoiceConfig<FilesizeConfig>(new FilesizeConfig()));
    this.data = DataType.RANDOM;
    this.concurrency = new ConcurrencyConfig();
    this.authentication = new AuthenticationConfig();
    this.client = new ClientConfig();
    this.stoppingConditions = new StoppingConditionsConfig();
    this.objectManager = new ObjectManagerConfig();
    this.virtualHost = false;
  }
}
