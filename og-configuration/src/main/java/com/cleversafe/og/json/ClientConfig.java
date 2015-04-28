/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.json;

public class ClientConfig {
  public int connectTimeout;
  public int soTimeout;
  public boolean soReuseAddress;
  public int soLinger;
  public boolean soKeepAlive;
  public boolean tcpNoDelay;
  public boolean persistentConnections;
  public boolean chunkedEncoding;
  public boolean expectContinue;
  public int waitForContinue;
  public int retryCount;
  public boolean requestSentRetry;
  public int writeThroughput;
  public int readThroughput;

  public ClientConfig() {
    this.connectTimeout = 0;
    this.soTimeout = 0;
    this.soReuseAddress = false;
    this.soLinger = -1;
    this.soKeepAlive = true;
    this.tcpNoDelay = true;
    this.persistentConnections = true;
    this.chunkedEncoding = false;
    this.expectContinue = false;
    this.waitForContinue = 3000;
    this.retryCount = 0;
    this.requestSentRetry = true;
    this.writeThroughput = 0;
    this.readThroughput = 0;
  }
}
