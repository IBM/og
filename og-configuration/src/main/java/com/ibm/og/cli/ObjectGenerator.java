/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.cli;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Thread.interrupted;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ListenableFuture;
import com.ibm.og.api.Request;
import com.ibm.og.api.Response;
import com.ibm.og.guice.ListModule;
import com.ibm.og.json.OGConfig;
import com.ibm.og.json.type.FilesizeConfigTypeAdapterFactory;
import com.ibm.og.test.condition.LoadTestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.og.cli.Application.Cli;
import com.ibm.og.guice.OGModule;
import com.ibm.og.json.type.ChoiceConfigTypeAdapterFactory;
import com.ibm.og.json.type.ContainerConfigTypeAdapterFactory;
import com.ibm.og.json.type.OperationConfigTypeAdapterFactory;
import com.ibm.og.json.type.SelectionConfigTypeAdapterFactory;
import com.ibm.og.util.json.type.SizeUnitTypeAdapter;
import com.ibm.og.util.json.type.TimeUnitTypeAdapter;
import com.ibm.og.util.json.type.CaseInsensitiveEnumTypeAdapterFactory;
import com.ibm.og.object.ObjectManager;
import com.ibm.og.statistic.Statistics;
import com.ibm.og.test.LoadTest;
import com.ibm.og.util.SizeUnit;
import com.ibm.og.util.Version;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.google.inject.spi.Message;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.ConfigurationException;
import com.google.inject.ProvisionException;
import com.google.inject.Stage;


/**
 * A cli for the Object Generator load tool
 * 
 * @since 1.0
 */
public class ObjectGenerator {
  private static final Logger _logger = LoggerFactory.getLogger(ObjectGenerator.class);
  private static final Logger _consoleLogger = LoggerFactory.getLogger("ConsoleLogger");
  private static final Logger _ogJsonLogger = LoggerFactory.getLogger("OGJsonLogger");
  private static final Logger _summaryJsonLogger = LoggerFactory.getLogger("SummaryJsonLogger");
  private static final Logger _exceptionLogger = LoggerFactory.getLogger("ExceptionLogger");
  private static final Logger _ogstatsLogger = LoggerFactory.getLogger("OgStatsLogger");
  private static final String LINE_SEPARATOR =
      "-------------------------------------------------------------------------------";

  private static final Gson gson = createGson();
  private static final Gson intervalGson = createIntervalGson();
  private static Injector injector;
  private static LoadTest test;
  private static ObjectManager objectManager;
  private static Statistics statistics;
  private static OGConfig ogConfig;
  private static Thread statsLogger;

  private static long timestampStart;
  private static long timestampStop;
  private static long timestampIntervalStart;


  private ObjectGenerator() {}

  public static void main(final String[] args) {
    timestampStart = System.currentTimeMillis();
    timestampIntervalStart = timestampStart;
    final OGGetOpt getopt = new OGGetOpt();
    final Cli cli = Application.cli("og", getopt, args);
    if (cli.shouldStop()) {
      if (cli.help()) {
        cli.printUsage();
      } else if (cli.version()) {
        cli.printVersion();
      } else if (cli.error()) {
        cli.printErrors();
        cli.printUsage();
        timestampStop = System.currentTimeMillis();
        logSummary(timestampStart, timestampStop, Application.TEST_CONFIG_ERROR, ImmutableList.of("Invalid Arguments"));
        Application.exit(Application.TEST_CONFIG_ERROR);

      }
      timestampStop = System.currentTimeMillis();
      logSummary(timestampStart, timestampStop, Application.TEST_SUCCESS, ImmutableList.of(Application.TEST_SUCCESS_MSG));
      Application.exit(Application.TEST_SUCCESS);
    }


    final CountDownLatch shutdownLatch = new CountDownLatch(1);

    logBanner();
    _consoleLogger.info("Configuring...");

    try {

     try {
       wireDependencies(cli, getopt);
     } catch (Exception e) {
       _logger.error("Exception while setting up dependencies", e);
       _consoleLogger.error("Test Error. See og.log for details");
       logConsoleException(e);
       logExceptionToFile(e);
       timestampStop = System.currentTimeMillis();
       logSummary(timestampStart, timestampStop, Application.TEST_CONFIG_ERROR, ImmutableList.of(String.format("Configuration error %s", e.getMessage())));
       Application.exit(Application.TEST_CONFIG_ERROR);
     }

      OGLog4jShutdownCallbackRegistry.setOGShutdownHook((new ShutdownHook(test, shutdownLatch)));
      // start log dump thread
      if (ogConfig.statsLogInterval > 0) {
        statsLogger = new Thread(new StatsLogger(), "stats-logger");
        statsLogger.start();
      }
      final LoadTestResult result = run(test, objectManager, statistics, gson);

      shutdownLatch.countDown();

      if (ogConfig.statsLogInterval > 0 && statsLogger.isAlive()) {
        statsLogger.interrupt();
      }

      // slight race here; if shutdown hook completes prior to the exit line below
      // if the test completes whether it passes or fails, the summary is written in the test results callback
      if (result.result < 0) {
        Application.exit(Application.TEST_ERROR);
      } else if (result.result > 0) {
        _logger.error("Test shutdown unsuccessful, terminated {} requests", result.result);
        Application.exit(Application.TEST_SHUTDOWN_ERROR);
      }
    } catch (final Exception e) {
      try {
        _logger.error("Exception while configuring and running test", e);
        _consoleLogger.error("Test Error. See og.log for details");
        if (ogConfig.statsLogInterval > 0 && statsLogger.isAlive()) {
          statsLogger.interrupt();
        }
        logConsoleException(e);
        logExceptionToFile(e);
        timestampStop = System.currentTimeMillis();
        logSummary(timestampStart, timestampStop, Application.TEST_ERROR,
                ImmutableList.of(String.format("Test error %s", e.getMessage())));
        _logger.warn("countdown shutdown latch in exception block");
      } finally {
        shutdownLatch.countDown();
        Application.exit(Application.TEST_ERROR);
      }
    }

    Application.exit(Application.TEST_SUCCESS);
  }

  /**
   *
   * @param cli CLI object created for this test
   * @param getopt Getoptions object reference
   *
   *
   * Note: If the dependency injection fails it would throw ConfigurationException or
   * ProvisionException.  In other cases, it throws ConfigurationException.
   *
   */
  private static void wireDependencies(Cli cli, OGGetOpt getopt) {

    final File json = new File(getopt.getOGConfigFileName());
    if (json == null) {
      _consoleLogger.error("A json configuration file is required");
      cli.printUsage();
      throw new ConfigurationException(ImmutableSet.of(new Message("OG Configuration json file is required")));
    }

    try {
      ogConfig = Application.fromJson(json, OGConfig.class, gson);
      _ogJsonLogger.info(gson.toJson(ogConfig));
    } catch (FileNotFoundException fe) {
      throw new RuntimeException("OGConfig file not found");
    }

    // dependency injection
    injector = createInjector(ogConfig);
    test = injector.getInstance(LoadTest.class);
    objectManager = injector.getInstance(ObjectManager.class);
    statistics = injector.getInstance(Statistics.class);

  }

  public static LoadTestResult run(final LoadTest test, final ObjectManager objectManager,
      final Statistics statistics, final Gson gson) {
    _logger.info("{}", test);
    _logger.info("{}", objectManager);
    _consoleLogger.info("Configured.");
    _consoleLogger.info("Test Running...");

    final LoadTestResult result = test.call();

    if (result.result == 0) {
      _consoleLogger.info("Test Completed.");
    } else {
      _consoleLogger.error("Test ended unsuccessfully. See og.log or exception.log for details");
    }

    shutdownObjectManager(objectManager);

    final Summary summary = logSummary(statistics, result.timestampStart, result.timestampFinish, result);

    logSummaryBanner();
    _consoleLogger.info("{}", summary.getSummaryStats().condensedSummary());

    return result;
  }

  public static void logConsoleException(final Exception e) {
    if (e instanceof ProvisionException) {
      logConsoleGuiceMessages(((ProvisionException) e).getErrorMessages());
    } else if (e instanceof CreationException) {
      logConsoleGuiceMessages(((CreationException) e).getErrorMessages());
    } else {
      _consoleLogger.error(e.getMessage());
    }
  }

  public static void logConsoleGuiceMessages(final Collection<Message> messages) {
    // guice exceptions contain many duplicate messages with slightly differing causes; we only want
    // unique messages logged to console so filter them here
    final Set<String> uniqueMessages = Sets.newHashSet();
    for (final Message message : messages) {
      if (!uniqueMessages.contains(message.getMessage())) {
        _consoleLogger.error(message.getMessage());
      }
      uniqueMessages.add(message.getMessage());
    }
  }

  public static void logExceptionToFile(final Exception e) {
    _exceptionLogger.error("Exception: ", e);
  }

  public static Gson createGson() {
    return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapterFactory(new CaseInsensitiveEnumTypeAdapterFactory())
            // SizeUnit and TimeUnit TypeAdapters must be registered after
            // CaseInsensitiveEnumTypeAdapterFactory in order to override the registration
            .registerTypeHierarchyAdapter(SizeUnit.class, new SizeUnitTypeAdapter().nullSafe())
            .registerTypeHierarchyAdapter(TimeUnit.class, new TimeUnitTypeAdapter().nullSafe())
            .registerTypeAdapterFactory(new OperationConfigTypeAdapterFactory())
            .registerTypeAdapterFactory(new SelectionConfigTypeAdapterFactory())
            .registerTypeAdapterFactory(new ChoiceConfigTypeAdapterFactory())
            .registerTypeAdapterFactory(new FilesizeConfigTypeAdapterFactory())
            .registerTypeAdapterFactory(new ContainerConfigTypeAdapterFactory()).setPrettyPrinting()
            .create();
  }

  public static Gson createIntervalGson() {
    return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapterFactory(new CaseInsensitiveEnumTypeAdapterFactory())
            // SizeUnit and TimeUnit TypeAdapters must be registered after
            // CaseInsensitiveEnumTypeAdapterFactory in order to override the registration
            .registerTypeHierarchyAdapter(SizeUnit.class, new SizeUnitTypeAdapter().nullSafe())
            .registerTypeHierarchyAdapter(TimeUnit.class, new TimeUnitTypeAdapter().nullSafe())
            .registerTypeAdapterFactory(new OperationConfigTypeAdapterFactory())
            .registerTypeAdapterFactory(new SelectionConfigTypeAdapterFactory())
            .registerTypeAdapterFactory(new ChoiceConfigTypeAdapterFactory())
            .registerTypeAdapterFactory(new FilesizeConfigTypeAdapterFactory())
            .registerTypeAdapterFactory(new ContainerConfigTypeAdapterFactory())
            .create();
  }

  public static Injector createInjector(final OGConfig ogConfig) {
    return Guice.createInjector(Stage.PRODUCTION, new OGModule(ogConfig), new ListModule(ogConfig));
  }

  public static void shutdownObjectManager(final ObjectManager objectManager) {
    try {
      objectManager.shutdown();
    } catch (final Exception e) {
      _logger.error("Error shutting down object manager", e);
    }
  }

  private static void logBanner() {
    final String bannerFormat = "%s%nObject Generator (%s)%n%s";
    final String banner =
        String.format(bannerFormat, LINE_SEPARATOR, Version.displayVersion(), LINE_SEPARATOR);
    _consoleLogger.info(banner);
  }

  private static void logSummaryBanner() {
    final String bannerFormat = "%s%nSummary%n%s";
    final String banner = String.format(bannerFormat, LINE_SEPARATOR, LINE_SEPARATOR);
    _consoleLogger.info(banner);
  }

  private static void logIntervalStatsBanner() {
    final String bannerFormat = "%s%nIntervalStats%n%s";
    final String banner = String.format(bannerFormat, LINE_SEPARATOR, LINE_SEPARATOR);
    _consoleLogger.info(banner);
  }

  private static Summary logSummary(final Statistics stats, final long timestampStart,
                                    final long timestampFinish, final LoadTestResult testResult)
  {
    int exitCode = Application.TEST_ERROR;
    if (testResult.result == 0) {
      exitCode = Application.TEST_SUCCESS;
    } else if (testResult.result > 0) {
      exitCode = Application.TEST_SHUTDOWN_ERROR;
    }
    final int requestsAborted = testResult.result > 0 ? testResult.result : 0;

    final Summary summary = new Summary(stats, timestampStart, timestampFinish, exitCode,
            testResult.result == 0 ? ImmutableList.of(Application.TEST_SUCCESS_MSG) : testResult.messages, requestsAborted);
    _summaryJsonLogger.info(gson.toJson(summary.getSummaryStats()));
    return summary;
  }

  private static Summary logSummary(final long timestampStart, final long timestampFinish,
                                    final int exitCode, ImmutableList<String> messages) {
    final Summary summary = new Summary(new Statistics(), timestampStart, timestampFinish, exitCode, messages, 0);
    _summaryJsonLogger.info(gson.toJson(summary.getSummaryStats()));
    return summary;
  }

  private static void dumpSummaryStats(Gson gson, Summary.SummaryOperationStats intervalStats, final long timestampStart, final long timestampFinish,
                                                        final int testStatus) {
    logIntervalStatsBanner();
    _ogstatsLogger.info(intervalGson.toJson(intervalStats));
    //_consoleLogger.info(gson.toJson(intervalStats));
    _consoleLogger.info("{}", intervalStats.toString());

  }

  private static class ShutdownHook extends Thread {
    private final LoadTest test;
    private final CountDownLatch shutdownLatch;

    public ShutdownHook(final LoadTest test, final CountDownLatch shutdownLatch) {
      this.test = checkNotNull(test);
      this.shutdownLatch = checkNotNull(shutdownLatch);
    }

    @Override
    public void run() {
      _logger.debug("og shutdown hook triggered, stopping test");
      this.test.stopTest();

      _logger.info("Waiting on shutdown lock");
      Uninterruptibles.awaitUninterruptibly(this.shutdownLatch);
      _logger.debug("Shutdown lock released, exiting og shutdown hook");
    }
  }

  private static class StatsLogger implements Runnable {
    IntervalSummary intervalSummary;
    @Override
    public void run() {
      boolean running = true;
      while (!interrupted()) {
        timestampStop = System.currentTimeMillis();
        if (intervalSummary == null) {
          intervalSummary = new IntervalSummary(statistics, timestampIntervalStart, timestampStop);
        } else {
          Summary.SummaryOperationStats istats = intervalSummary.intervalStats(statistics, timestampIntervalStart, timestampStop);
          dumpSummaryStats(gson, istats, timestampStart, timestampStop, Application.TEST_SUCCESS);
          timestampIntervalStart = System.currentTimeMillis();
        }
        try {
          Thread.sleep(ogConfig.statsLogInterval * 1000);
        } catch(InterruptedException ie) {
          Thread.currentThread().interrupt();
          _logger.info("StatsLogger thread interrupted");
        }
      }
      _logger.info("StatsLogger Thread done after interruption");
    }
  }

}
