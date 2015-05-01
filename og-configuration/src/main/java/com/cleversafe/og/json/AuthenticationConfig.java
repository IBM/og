/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.json;

import com.cleversafe.og.s3.v4.AWSAuthV4Chunked;


public class AuthenticationConfig {
  public AuthType type;
  public String username;
  public String password;
  public int awsChunkSize;
  public boolean awsChunked;

  public AuthenticationConfig() {
    this.type = AuthType.BASIC;
    this.username = null;
    this.password = null;
    this.awsChunkSize = AWSAuthV4Chunked.DEFAULT_CHUNK_SIZE;
    this.awsChunked = false;
  }
}
