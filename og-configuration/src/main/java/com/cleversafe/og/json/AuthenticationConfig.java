/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.json;

public class AuthenticationConfig {
  public AuthType type;
  public CredentialSource credentialSource;
  public String username;
  public String password;
  public String keystoneToken;
  public String credentialFile;
  public boolean awsChunked;
  public int awsCacheSize;

  public AuthenticationConfig() {
    this.type = AuthType.NONE;
    this.credentialSource = CredentialSource.CONFIG;
    this.username = null;
    this.password = null;
    this.keystoneToken = null;
    this.credentialFile = null;
    this.awsChunked = false;
    this.awsCacheSize = 0;
  }
}
