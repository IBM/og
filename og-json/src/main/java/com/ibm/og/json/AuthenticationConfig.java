/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.json;

import com.ibm.og.api.AuthType;

public class AuthenticationConfig {
  public AuthType type;
  public CredentialSource credentialSource;
  public String username;
  public String password;
  public String keystoneToken;
  public String iamToken;
  public String credentialFile;
  public String account;
  public boolean awsChunked;
  public int awsCacheSize;

  public AuthenticationConfig() {
    this.type = AuthType.NONE;
    this.credentialSource = CredentialSource.CONFIG;
    this.username = null;
    this.password = null;
    this.keystoneToken = null;
    this.iamToken = null;
    this.credentialFile = null;
    this.account = null;
    this.awsChunked = false;
    this.awsCacheSize = 0;
  }
}
