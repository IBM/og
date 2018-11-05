/*
 * Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.json;

import java.util.Map;

import com.google.common.collect.Maps;
import com.ibm.og.api.BodySource;

public class OperationConfig {
  public double weight;
  public SelectionConfig<String> host;
  public ObjectConfig object;
  public ObjectConfig sourceObject;
  public SelectionConfig<FilesizeConfig> filesize;
  public Map<String, SelectionConfig<String>> headers;
  public Map<String, String> parameters;
  public Map<String, SelectionConfig<String>> weightedParameters;
  public BodySource body;
  public ContainerConfig container;
  public ContainerConfig sourceContainer;
  public MultipartConfig upload;
  public boolean sseCSource;
  public boolean sseCDestination;
  public LegalHold legalHold;
  public SelectionConfig<RetentionConfig> retention;
  public boolean contentMd5;
  public Integer objectRestorePeriod;
  public Integer archiveTransitionPeriod;
  public SelectionConfig<RetentionConfig> containerMinimumRetention;
  public SelectionConfig<RetentionConfig> containerMaximumRetention;
  public SelectionConfig<RetentionConfig> containerDefaultRetention;


  public OperationConfig(final double weight) {
    this();
    this.weight = weight;
  }

  public OperationConfig() {
    this.weight = 0.0;
    this.host = null;
    this.object = new ObjectConfig();
    this.sourceObject = new ObjectConfig();
    this.filesize = null;
    this.headers = Maps.newLinkedHashMap();
    this.parameters = Maps.newLinkedHashMap();
    this.weightedParameters = Maps.newLinkedHashMap();
    this.body = BodySource.NONE;
    this.container = new ContainerConfig();
    this.sourceContainer = new ContainerConfig();
    this.upload = new MultipartConfig();
    this.legalHold = null;
    this.retention = null;
    this.contentMd5 = false;
    this.objectRestorePeriod = 1;
    this.archiveTransitionPeriod = 1;
  }
}
