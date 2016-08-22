/*
 * Copyright (C) 2005-2016 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.cli;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.util.Version;
import com.google.common.base.Charsets;
import com.google.gson.Gson;

/**
 * A utility class for creating an application cli
 * 
 * @since 1.0
 */
public class Application {
  private static final Logger _logger = LoggerFactory.getLogger(Application.class);
  public static final int TEST_ERROR = 1;

  private Application() {}

  /**
   * Creates a Cli instance
   * 
   * @param name a textual name that describes this cli
   * @param args command line arguments
   * @return a Cli instance
   * @throws NullPointerException if name, or args are null
   */
  public static Cli cli(final String name, final String[] args) {
    GetOpt getopt = ArgumentProcessorFactory.makeArgumentProcessor(name);
    return new Cli(name, getopt, args);
  }

  /**
   * A helper class for working with command line arguments
   * 
   * @since 1.0
   */
  public static class Cli {
    private static final Logger _consoleLogger = LoggerFactory.getLogger("ConsoleLogger");
    private final String name;
    private GetOpt getopt;

    /**
     * Creates an instance
     * 
     * @param name a textual name that describes this cli
     * @param args command line arguments
     * @return a Cli instance
     * @throws NullPointerException if name, or args are null
     */
    private Cli(final String name, final GetOpt getopt, final String[] args) {
      this.name = checkNotNull(name);
      checkNotNull(args);
      checkNotNull(getopt);
      try {
        this.getopt = getopt;
        getopt.processArguments(name, args);
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
      return getopt.isError() || getopt.getHelp() || getopt.getVersion();
    }

    /**
     * Determines if any error occurred during argument parsing
     * 
     * @return true if cli parsing errors occurred; false otherwise
     */
    public boolean error() {
      return getopt.isError();
    }

    /**
     * Determines if a help flag was passed into command line args
     * 
     * @return true if a help flag is present; false otherwise
     */
    public boolean help() {
      return getopt.getHelp();
    }

    /**
     * Determines if a version flag was passed into command line args
     * 
     * @return true if a version plag is present; false otherwise
     */
    public boolean version() {
      return getopt.getVersion();
    }


    public GetOpt getOpt() {
      return getopt;
    }


    /**
     * Generates and logs a suitable cli usage block to the console
     */
    public void printUsage() {
      //_consoleLogger.info("Usage: {} {}", this.name, this.jsap.getUsage());
      StringBuilder sb = new StringBuilder();
      getopt.usage(sb);
      _consoleLogger.info(sb.toString());
    }

    /**
     * Generates and logs a suitable errors block to console
     */
    public void printErrors() {
      _consoleLogger.error("{}", getopt.getErrorMsg());
    }

    /**
     * Generates and logs a suitable version block to console
     */
    public void printVersion() {
      _consoleLogger.info(Version.displayVersion());
    }
  }

  /**
   *  Helper Factory class to create the appropriate Argument Processor based on the application name
   */
  private static class ArgumentProcessorFactory {

    private ArgumentProcessorFactory() {}

    /***
     *
     * @param appName a textual name that describes the cli
     * @return a Cli instance
     */
    public static GetOpt makeArgumentProcessor(String appName) {

      if (appName.contentEquals("application")) {
        // just basic Argument process that supports options help and version - for testing only
        return new GetOpt();
      } else if (appName.contentEquals("og")) {
        // Arg processor for Object Generator
        return new OGGetOpt();
      } else if (appName.contentEquals("object-file")) {
        // Arg processor for Object File
        return new ObjectFileGetOpt();
      } else {
        // throw IllegalArgument exception
        throw new IllegalArgumentException("Illegal application Name " + appName);
      }

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
      checkArgument(url != null, "Could not find configuration file on classpath [%s]",
          resourceName);

      return url.toURI();
    } catch (final URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Creates an object from an underlying json configuration
   * 
   * @param json a user specified json configuration file
   * @param cls the class to deserialize the json configuration into
   * @param gson a gson instance to perform deserialization with
   * @return A deserialized user object
   * @throws FileNotFoundException if no json configuration can be found at the provided location
   */
  public static <T> T fromJson(final File json, final Class<T> cls, final Gson gson)
      throws FileNotFoundException {
    checkNotNull(json);
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
    _logger.info("Exiting with exit code [{}]", exitCode);
    System.exit(exitCode);
  }
}
