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

/**
 * A utility class for creating an application cli
 * 
 * @since 1.0
 */
public class Application {
  public static final int TEST_ERROR = 1;

  private Application() {}

  /**
   * Creates a Cli instance
   * 
   * @param name a textual name that describes this cli
   * @param jsapResourceName the classpath location for this cli's jsap configuration
   * @param args command line arguments
   * @return a Cli instance
   * @throws NullPointerException if name, jsapResourceName or args are null
   */
  public static Cli cli(final String name, final String jsapResourceName, final String[] args) {
    return new Cli(name, jsapResourceName, args);
  }

  /**
   * A helper class for working with command line arguments
   * 
   * @since 1.0
   */
  public static class Cli {
    private static final Logger _consoleLogger = LoggerFactory.getLogger("ConsoleLogger");
    private final String name;
    private final JSAP jsap;
    private final JSAPResult jsapResult;

    /**
     * Creates an instance
     * 
     * @param name a textual name that describes this cli
     * @param jsapResourceName the classpath location for this cli's jsap configuration
     * @param args command line arguments
     * @return a Cli instance
     * @throws NullPointerException if name, jsapResourceName or args are null
     */
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

    /**
     * Determines if the caller should stop, whether due to errors, help, or version commands
     * 
     * @return true if the caller should stop; false otherwise
     */
    public boolean shouldStop() {
      return error() || help() || version();
    }

    /**
     * Determines if any error occurred during argument parsing
     * 
     * @return true if cli parsing errors occurred; false otherwise
     */
    public boolean error() {
      return !this.jsapResult.success();
    }

    /**
     * Determines if a help flag was passed into command line args
     * 
     * @return true if a help flag is present; false otherwise
     */
    public boolean help() {
      return this.jsapResult.getBoolean("help");
    }

    /**
     * Determines if a version flag was passed into command line args
     * 
     * @return true if a version plag is present; false otherwise
     */
    public boolean version() {
      return this.jsapResult.getBoolean("version");
    }

    /**
     * Makes available the underlying {@code JSAPResult } result object
     * 
     * @return the underlying cli results object
     */
    public JSAPResult flags() {
      return this.jsapResult;
    }

    /**
     * Generates and logs a suitable cli usage block to the console
     */
    public void printUsage() {
      _consoleLogger.info("Usage: {} {}", this.name, this.jsap.getUsage());
      _consoleLogger.info(this.jsap.getHelp());
    }

    /**
     * Generates and logs a suitable errors block to console
     */
    public void printErrors() {
      @SuppressWarnings("rawtypes")
      final Iterator errs = this.jsapResult.getErrorMessageIterator();
      while (errs.hasNext()) {
        _consoleLogger.error("{}", errs.next());
      }
    }

    /**
     * Generates and logs a suitable version block to console
     */
    public void printVersion() {
      _consoleLogger.info(Version.displayVersion());
    }
  }

  /**
   * Creates a URI that points to a classpath resource
   * 
   * @param resourceName the name of a resource on the classpath
   * @return A URI pointing to a classpath resource
   * @throws NullPointerException if resourceName is null
   */
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

  /**
   * Creates an object from an underlying json configuration. If a user specified file location is
   * provided use it; otherwise use a default configuration file location
   * 
   * @param userJson a file pointing to a user specified json configuration file
   * @param defaultJson a file pointing to a default json configuration file
   * @param cls the class to deserialize the json configuration into
   * @param gson a gson instance to perform deserialization with
   * @return A deserialized user object
   * @throws FileNotFoundException if no json configuration can be found at the provided location
   */
  public static <T> T fromJson(final File userJson, final File defaultJson, final Class<T> cls,
      final Gson gson) throws FileNotFoundException {
    final File json = userJson != null ? userJson : checkNotNull(defaultJson);
    checkNotNull(cls);
    checkNotNull(gson);

    final Reader reader = new InputStreamReader(new FileInputStream(json), Charsets.UTF_8);
    return gson.fromJson(reader, cls);
  }

  /**
   * Calls {@code System.exit} to exit the vm
   * 
   * @param exitCode the exit code to pass to System.exit
   */
  public static void exit(final int exitCode) {
    System.exit(exitCode);
  }
}
