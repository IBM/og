/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.json;

import java.util.List;

public class ClientConfig {
  public int connectTimeout;
  public int soTimeout;
  public boolean soReuseAddress;
  public int soLinger;
  public boolean soKeepAlive;
  public boolean tcpNoDelay;
  public int soSndBuf;
  public int soRcvBuf;
  public boolean persistentConnections;
  public int validateAfterInactivity;
  public int maxIdleTime;
  public boolean chunkedEncoding;
  public boolean expectContinue;
  public int waitForContinue;
  public int retryCount;
  public boolean requestSentRetry;
  public List<String> protocols;
  public List<String> cipherSuites;
  public String keyStore;
  public String keyStorePassword;
  public String keyPassword;
  public String trustStore;
  public String trustStorePassword;
  public boolean trustSelfSignedCertificates;
  public int dnsCacheTtl;
  public int dnsCacheNegativeTtl;
  public int writeThroughput;
  public int readThroughput;
  public String proxy;
  public int proxyPort;
  public String proxyScheme;
  public String proxyUser;
  public String proxyPassword;

  public ClientConfig() {
    this.connectTimeout = 0;
    this.soTimeout = 0;
    this.soReuseAddress = false;
    this.soLinger = -1;
    this.soKeepAlive = true;
    this.tcpNoDelay = true;
    this.soSndBuf = 0;
    this.soRcvBuf = 0;
    this.persistentConnections = true;
    this.validateAfterInactivity = 10000;
    this.maxIdleTime = 60000;
    this.chunkedEncoding = false;
    this.expectContinue = false;
    this.waitForContinue = 3000;
    this.retryCount = 0;
    this.requestSentRetry = true;
    this.protocols = null;
    this.cipherSuites = null;
    this.keyStore = null;
    this.keyStorePassword = null;
    this.keyPassword = null;
    this.trustStore = null;
    this.trustStorePassword = null;
    this.trustSelfSignedCertificates = false;
    this.dnsCacheTtl = 60;
    this.dnsCacheNegativeTtl = 10;
    this.writeThroughput = 0;
    this.readThroughput = 0;
    this.proxyPort = 3128;
    this.proxyScheme = "https";
  }
}
