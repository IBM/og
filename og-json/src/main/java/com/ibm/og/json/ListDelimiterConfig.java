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

public class ListDelimiterConfig {
  public final String delimiterCharacter;

  public ListDelimiterConfig(String delimiterCharacter) {
    this.delimiterCharacter = delimiterCharacter;
  }
}
