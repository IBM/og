//
// Copyright (C) 2005-2011 Cleversafe, Inc. All rights reserved.
//
// Contact Information:
// Cleversafe, Inc.
// 222 South Riverside Plaza
// Suite 1700
// Chicago, IL 60606, USA
//
// licensing@cleversafe.com
//
// END-OF-HEADER
//
// -----------------------
// @author: rveitch
//
// Date: Jun 15, 2014
// ---------------------

package com.cleversafe.og.json;

public class ClientConfig {
  int connectTimeout;
  int soTimeout;
  boolean soReuseAddress;
  int soLinger;
  boolean soKeepAlive;
  boolean tcpNoDelay;
  boolean persistentConnections;
  boolean chunkedEncoding;
  boolean expectContinue;
  int waitForContinue;
  int writeThroughput;
  int readThroughput;

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
    this.writeThroughput = 0;
    this.readThroughput = 0;
  }

  public int getConnectTimeout() {
    return this.connectTimeout;
  }

  public int getSoTimeout() {
    return this.soTimeout;
  }

  public boolean isSoReuseAddress() {
    return this.soReuseAddress;
  }

  public int getSoLinger() {
    return this.soLinger;
  }

  public boolean isSoKeepAlive() {
    return this.soKeepAlive;
  }

  public boolean isTcpNoDelay() {
    return this.tcpNoDelay;
  }

  public boolean isPersistentConnections() {
    return this.persistentConnections;
  }

  public boolean isChunkedEncoding() {
    return this.chunkedEncoding;
  }

  public boolean isExpectContinue() {
    return this.expectContinue;
  }

  public int getWaitForContinue() {
    return this.waitForContinue;
  }

  public int getWriteThroughput() {
    return this.writeThroughput;
  }

  public int getReadThroughput() {
    return this.readThroughput;
  }
}
