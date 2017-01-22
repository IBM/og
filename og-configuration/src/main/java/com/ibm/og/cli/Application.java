/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.cli;

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

import com.ibm.og.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.gson.Gson;

import com.beust.jcommander.JCommander;

/**
 * A utility class for creating an application cli
 * 
 * @since 1.0
 */
public class Application {
  private static final Logger _logger = LoggerFactory.getLogger(Application.class);
  public static final int TEST_SUCCESS = 0;
  public static final int TEST_ERROR = 1;
  public static final int TEST_CONFIG_ERROR = 2;

  public static final String TEST_SUCCESS_MSG = "Test exited normally";

  private Application() {}

  /**
   * Creates a Cli instance
   * 
   * @param name a textual name that describes this cli
   * @param args command line arguments
   * @return a Cli instance
   * @throws NullPointerException if name, or args are null
   */
  public static Cli cli(final String name, final GetOpt getopt, final String[] args) {
    return new Cli(name, getopt, args);
  }

  /**
   * A helper class for working with command line arguments
   * 
   * @since 1.0
   */
  public static class Cli <T extends GetOpt> {
    private static final Logger _consoleLogger = LoggerFactory.getLogger("ConsoleLogger");
    private final String name;
    private T getopt;
    private JCommander jc;
    private boolean error;
    private String errorMsg;

    /**
     * Creates an instance
     * 
     * @param name a textual name that describes this cli
     * @param args command line arguments
     * @return a Cli instance
     * @throws NullPointerException if name, or args are null
     */
    private Cli(final String name, final T getopt, final String[] args) {
      this.name = checkNotNull(name);
      checkNotNull(args);
      checkNotNull(getopt);
      try {
        this.getopt = getopt;
        processArguments(name, args);
      } catch (final Exception e) {
        throw new IllegalArgumentException(e);
      }
    }

    private void processArguments (String progName,  String args[]) {

      try {
        jc = new JCommander(getopt);
        jc.setProgramName(progName);
        jc.parse(args);
        getopt.validate();
      } catch(RuntimeException re) {
        // record error in the state to match with the existing semantics
        error = true;
        errorMsg = re.getLocalizedMessage();
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
      // error is set if the JCommander parse fails and throws RuntimeException
      return error;
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


    /**
     * Generates and logs a suitable cli usage block to the console
     */
    public void printUsage() {
      //_consoleLogger.info("Usage: {} {}", this.name, this.jsap.getUsage());
      StringBuilder sb = new StringBuilder();
      jc.usage(sb);
      _consoleLogger.info(sb.toString());
    }

    /**
     * Generates and logs a suitable errors block to console
     */
    public void printErrors() {
      _consoleLogger.error("{}", errorMsg);
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
