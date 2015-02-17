/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.cli;

import java.util.Locale;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.joda.time.format.DateTimeFormat;

@Plugin(name = "og", category = "Lookup")
public class OGDatetimeLookup implements StrLookup {
  private static final String DATETIME = DateTimeFormat.forPattern("yyyy-MM-dd_HH.mm.ss")
      .withLocale(Locale.US).print(System.currentTimeMillis());

  public OGDatetimeLookup() {}

  @Override
  public String lookup(final String key) {
    return DATETIME;
  }

  @Override
  public String lookup(final LogEvent event, final String key) {
    return DATETIME;
  }
}
