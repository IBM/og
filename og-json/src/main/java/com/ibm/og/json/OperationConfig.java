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
  public SelectionConfig<FilesizeConfig> range;
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
  public String retentionMode;
  public boolean contentMd5;
  public Integer objectRestorePeriod;
  public Integer archiveTransitionPeriod;
  public SelectionConfig<RetentionConfig> containerMinimumRetention;
  public SelectionConfig<RetentionConfig> containerMaximumRetention;
  public SelectionConfig<RetentionConfig> containerDefaultRetention;
  public SelectionConfig<PrefixConfig> prefix;
  public SelectionConfig<ObjectDelimiterConfig> delimiter; // for write operations
  public SelectionConfig<ListDelimiterConfig> listDelimiter;
  public SelectionConfig<ListSessionConfig> listSessionConfig;
  public SelectionConfig<WriteSelectBodyConfig> writeSelectBodyConfig;
  public Integer minimumListSessions;
  public Integer multideleteCount;
  public boolean multideleteQuiet;
  public String staticWebsiteVirtualHostSuffix;
  public ObjectTagsConfig tagsConfiguration;
  public ObjectVersionSelection objectVersionSelection;

  public LegalHoldStatusSelection objectLegalHoldStatusSelection;

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
    this.prefix = null;
    this.delimiter = null;
    this.listDelimiter = null;
    this.listSessionConfig = new SelectionConfig<ListSessionConfig>();
    this.listSessionConfig.selection = SelectionType.RANDOM;
    ListSessionConfig listSession = new ListSessionConfig();
    listSession.maxChainedRequests = 1;
    listSession.requestType = "UNCHAINED";
    listSession.startFromBeginning = false;
    this.listSessionConfig.choices.add(0, new ChoiceConfig<ListSessionConfig>(listSession));
    this.minimumListSessions = 1;
    this.multideleteCount = 1;
    this.multideleteQuiet = true;
    this.staticWebsiteVirtualHostSuffix = null;
    this.tagsConfiguration = null;
    this.objectVersionSelection = null;
    this.objectLegalHoldStatusSelection = null;
  }
}
