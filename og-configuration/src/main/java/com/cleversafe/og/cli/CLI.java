//
// Copyright (C) 2005-2011 Cleversafe, Inc. All rights reserved.
//
// Contact Information:
// Cleversafe, Inc.
// 222 South Riverside Plaza
// Suite 1700
// Chicago, IL 60606, USA
//
// licensing@cleversafe.com
//
// END-OF-HEADER
//
// -----------------------
// @author: rveitch
//
// Date: Feb 13, 2014
// ---------------------

package com.cleversafe.og.cli;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.util.Version;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;

public abstract class CLI {
  private static final Logger _logger = LoggerFactory.getLogger(CLI.class);
  protected static final Logger _consoleLogger = LoggerFactory.getLogger("ConsoleLogger");
  protected static final int CONFIGURATION_ERROR = 1;
  protected static final int UNKNOWN_ERROR = 2;
  protected String client;
  protected JSAP jsap;
  protected JSAPResult jsapResult;
  protected boolean error;
  protected int exitCode;

  public CLI(final String client, final String jsapResourceName, final String[] args) {
    try {
      this.client = checkNotNull(client);
      this.jsap = new JSAP(getResource(checkNotNull(jsapResourceName)).toURL());
      this.jsapResult = this.jsap.parse(checkNotNull(args));
      checkArgument(this.jsapResult.success(), "Error processing cli flags");
    } catch (final Exception e) {
      _logger.error("", e);
      this.error = true;
      this.exitCode = CONFIGURATION_ERROR;
    }
  }

  public abstract boolean start();

  public boolean shouldStop() {
    return error() || help() || version();
  }

  public boolean error() {
    return this.error;
  }

  public boolean help() {
    return this.jsapResult.getBoolean("help");
  }

  public boolean version() {
    return this.jsapResult.getBoolean("version");
  }

  public void printUsage() {
    _consoleLogger.info("Usage: {} {}", this.client, this.jsap.getUsage());
    _consoleLogger.info(this.jsap.getHelp());
  }

  public void printErrors() {
    @SuppressWarnings("rawtypes")
    final Iterator errs = this.jsapResult.getErrorMessageIterator();
    while (errs.hasNext()) {
      _consoleLogger.error("{}", errs.next());
    }
  }

  public void printVersion() {
    _consoleLogger.info(Version.displayVersion());
  }

  public int exitCode() {
    return this.exitCode;
  }

  public void exit(final int exitCode) {
    System.exit(exitCode);
  }

  protected static URI getResource(final String resourceName) {
    URL url = null;
    try {
      url = ClassLoader.getSystemResource(resourceName);

      if (url == null)
        throw new IllegalStateException(String.format(
            "Could not find configuration file on classpath [%s]", resourceName));

      return url.toURI();
    } catch (final URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
