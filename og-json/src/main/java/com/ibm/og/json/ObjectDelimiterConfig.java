/* Copyright (c) IBM Corporation 2018. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.json;

/**
 * A guice configuration module for wiring up list delimiter configuration
 *
 * @since 1.8.4
 */

public class ObjectDelimiterConfig {

  public final DelimChar[] delimChars;

  public ObjectDelimiterConfig(DelimChar[] delimChars) {
    this.delimChars = delimChars;
  }

  public static class DelimChar {
    public String value;
    public int[] positions;
  }

}
