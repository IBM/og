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
import java.security.Provider;
import java.security.Security;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.cli.Application.Cli;
import com.cleversafe.og.guice.OGModule;
import com.cleversafe.og.json.OGConfig;
import com.cleversafe.og.json.type.CaseInsensitiveEnumTypeAdapterFactory;
import com.cleversafe.og.json.type.ChoiceConfigTypeAdapterFactory;
import com.cleversafe.og.json.type.ContainerConfigTypeAdapterFactory;
import com.cleversafe.og.json.type.FilesizeConfigTypeAdapterFactory;
import com.cleversafe.og.json.type.OperationConfigTypeAdapterFactory;
import com.cleversafe.og.json.type.SelectionConfigTypeAdapterFactory;
import com.cleversafe.og.json.type.SizeUnitTypeAdapter;
import com.cleversafe.og.json.type.TimeUnitTypeAdapter;
import com.cleversafe.og.object.ObjectManager;
import com.cleversafe.og.statistic.Statistics;
import com.cleversafe.og.test.LoadTest;
import com.cleversafe.og.test.condition.LoadTestResult;
import com.cleversafe.og.util.SizeUnit;
import com.cleversafe.og.util.Version;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.ProvisionException;
import com.google.inject.Stage;
import com.google.inject.spi.Message;
import org.apache.security.juice.provider.JuiCEProviderOpenSSL;

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
  private static final String LINE_SEPARATOR =
      "-------------------------------------------------------------------------------";

  private ObjectGenerator() {}

  public static void main(final String[] args) {
    final Cli cli = Application.cli("og", "og.jsap", args);
    if (cli.shouldStop()) {
      if (cli.help()) {
        cli.printUsage();
      } else if (cli.version()) {
        cli.printVersion();
      } else if (cli.error()) {
        cli.printErrors();
        cli.printUsage();
        Application.exit(Application.TEST_ERROR);
      }
      Application.exit(0);
    }

    final Gson gson = createGson();
    final CountDownLatch shutdownLatch = new CountDownLatch(1);

    logBanner();
    _consoleLogger.info("Configuring...");

    try {
      final File json = cli.flags().getFile("og_config");
      if (json == null) {
        _consoleLogger.error("A json configuration file is required");
        cli.printUsage();
        Application.exit(Application.TEST_ERROR);
      }

      final OGConfig ogConfig = Application.fromJson(json, OGConfig.class, gson);
      _ogJsonLogger.info(gson.toJson(ogConfig));

      final Injector injector = createInjector(ogConfig);
      final LoadTest test = injector.getInstance(LoadTest.class);
      final ObjectManager objectManager = injector.getInstance(ObjectManager.class);
      final Statistics statistics = injector.getInstance(Statistics.class);
      OGLog4jShutdownCallbackRegistry.setOGShutdownHook((new ShutdownHook(test, shutdownLatch)));

      final Provider juiceProvider;
      try {
        juiceProvider = JuiCEProviderOpenSSL.getInstance();
        Security.removeProvider(JuiCEProviderOpenSSL.NAME);
        Security.insertProviderAt(juiceProvider, 1);
        _logger.info("Using the JuiCE provider");
      } catch (final Exception e) {
        _logger.warn("The JuiCE provider is not available on this platform.", e);
      }

      final LoadTestResult result = run(test, objectManager, statistics, gson);

      shutdownLatch.countDown();

      // slight race here; if shutdown hook completes prior to the exit line below
      if (!result.success) {
        Application.exit(Application.TEST_ERROR);
      }
    } catch (final Exception e) {
      _logger.error("Exception while configuring and running test", e);
      _consoleLogger.error("Test Error. See og.log for details");
      logConsoleException(e);
      Application.exit(Application.TEST_ERROR);
    }

    Application.exit(0);
  }

  public static LoadTestResult run(final LoadTest test, final ObjectManager objectManager,
      final Statistics statistics, final Gson gson) {
    _logger.info("{}", test);
    _logger.info("{}", objectManager);
    _consoleLogger.info("Configured.");
    _consoleLogger.info("Test Running...");

    final LoadTestResult result = test.call();

    if (result.success) {
      _consoleLogger.info("Test Completed.");
    } else {
      _consoleLogger.error("Test ended unsuccessfully. See og.log for details");
    }

    shutdownObjectManager(objectManager);

    final Summary summary = new Summary(statistics, result.timestampStart, result.timestampFinish);
    _summaryJsonLogger.info(gson.toJson(summary.getSummaryStats()));

    logSummaryBanner();
    _consoleLogger.info("{}", summary);

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

  public static Injector createInjector(final OGConfig ogConfig) {
    return Guice.createInjector(Stage.PRODUCTION, new OGModule(ogConfig));
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
}
