/* Copyright (c) IBM Corporation 2018. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.json;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import java.nio.charset.Charset;
import java.util.Objects;

/**
 * A guice configuration module for wiring up list session configuration
 *
 * @since 1.8.4
 */

public class ListSessionConfig {

  public String requestType;
  public boolean startFromBeginning;
  public int maxChainedRequests;

  public ListSessionConfig() {

  }

  public ListSessionConfig(String requestType, boolean startFromBeginning, int maxChainedRequests) {
    this.requestType = requestType;
    this.startFromBeginning = startFromBeginning;
    this.maxChainedRequests = maxChainedRequests;
  }

  @Override
  public int hashCode() {
    HashFunction hf = Hashing.md5();

    Hasher hc = hf.newHasher().putBytes(requestType.getBytes()).putBoolean(startFromBeginning).putInt(maxChainedRequests);
    HashCode hashCode = hc.hash();
    int hash = hashCode.asInt();
    return hash;
  }

}
