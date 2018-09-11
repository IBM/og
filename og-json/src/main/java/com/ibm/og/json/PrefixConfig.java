/* Copyright (c) IBM Corporation 2018. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.json;

import java.util.concurrent.TimeUnit;

/**
 * A guice configuration module for wiring up list prefix configuration
 *
 * @since 1.8.4
 */
public class PrefixConfig {

  public final String prefixString;
  public final boolean useMarker;
  public final int numChars;

  public PrefixConfig(String prefixString, int numChars, boolean useMarker) {
    this.prefixString = prefixString;
    this.numChars = numChars;
    this.useMarker = useMarker;
  }

}
