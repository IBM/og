/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.cli;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.util.Version;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;

public class Application {
  public static final int EXIT_CONFIGURATION = 1;

  private Application() {}

  public static Cli cli(final String name, final String jsapResourceName, final String[] args) {
    return new Cli(name, jsapResourceName, args);
  }

  public static class Cli {
    private static final Logger _consoleLogger = LoggerFactory.getLogger("ConsoleLogger");
    private final String name;
    private final JSAP jsap;
    private final JSAPResult jsapResult;

    private Cli(final String name, final String jsapResourceName, final String[] args) {
      this.name = checkNotNull(name);
      checkNotNull(jsapResourceName);
      checkNotNull(args);
      try {
        this.jsap = new JSAP(Application.getResource(jsapResourceName).toURL());
        this.jsapResult = this.jsap.parse(args);
      } catch (final Exception e) {
        throw new IllegalArgumentException(e);
      }
    }

    public boolean shouldStop() {
      return error() || help() || version();
    }

    public boolean error() {
      return !this.jsapResult.success();
    }

    public boolean help() {
      return this.jsapResult.getBoolean("help");
    }

    public boolean version() {
      return this.jsapResult.getBoolean("version");
    }

    public JSAPResult flags() {
      return this.jsapResult;
    }

    public void printUsage() {
      _consoleLogger.info("Usage: {} {}", this.name, this.jsap.getUsage());
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
  }

  public static URI getResource(final String resourceName) {
    checkNotNull(resourceName);
    try {
      final URL url = ClassLoader.getSystemResource(resourceName);

      if (url == null)
        throw new IllegalArgumentException(String.format(
            "Could not find configuration file on classpath [%s]", resourceName));

      return url.toURI();
    } catch (final URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static <T> T fromJson(final File userJson, final File defaultJson, final Class<T> cls,
      final Gson gson) throws FileNotFoundException {
    final File json = userJson != null ? userJson : checkNotNull(defaultJson);
    checkNotNull(cls);
    checkNotNull(gson);

    final Reader reader = new InputStreamReader(new FileInputStream(json), Charsets.UTF_8);
    return gson.fromJson(reader, cls);
  }

  public static void exit(final int exitCode) {
    System.exit(exitCode);
  }
}
