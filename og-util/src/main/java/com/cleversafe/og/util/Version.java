/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.cleversafe.og.util;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class for working with OG versions
 * 
 * @since 1.0
 */
public class Version {
  private static final Logger _logger = LoggerFactory.getLogger(Version.class);
  private static final String DISPLAY_VERSION;

  static {
    String displayVersion;
    try {
      displayVersion = ResourceBundle.getBundle("og").getString("display.version");
    } catch (final MissingResourceException e) {
      _logger.error("Unable to retrieve display version", e);
      displayVersion = "0.0.0";
    }
    DISPLAY_VERSION = displayVersion;
  }

  private Version() {}

  /**
   * Returns a version string suitable for display in logs and other output
   * 
   * @return a version string
   */
  public static String displayVersion() {
    return DISPLAY_VERSION;
  }
}
