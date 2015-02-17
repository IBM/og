/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.json;


public class AuthenticationConfig {
  AuthType type;
  String username;
  String password;

  public AuthenticationConfig() {
    this.type = AuthType.BASIC;
    this.username = null;
    this.password = null;
  }

  public AuthType getType() {
    return this.type;
  }

  public String getUsername() {
    return this.username;
  }

  public String getPassword() {
    return this.password;
  }
}
