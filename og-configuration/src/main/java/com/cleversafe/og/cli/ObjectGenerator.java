/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.cli;

import java.io.File;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.api.Client;
import com.cleversafe.og.cli.Application.Cli;
import com.cleversafe.og.guice.OGModule;
import com.cleversafe.og.json.OGConfig;
import com.cleversafe.og.json.type.CaseInsensitiveEnumTypeAdapterFactory;
import com.cleversafe.og.json.type.ConcurrencyConfigTypeAdapterFactory;
import com.cleversafe.og.json.type.FilesizeConfigListTypeAdapterFactory;
import com.cleversafe.og.json.type.FilesizeConfigTypeAdapterFactory;
import com.cleversafe.og.json.type.HostConfigListTypeAdapterFactory;
import com.cleversafe.og.json.type.HostConfigTypeAdapterFactory;
import com.cleversafe.og.json.type.OperationConfigTypeAdapterFactory;
import com.cleversafe.og.json.type.SelectionConfigTypeAdapterFactory;
import com.cleversafe.og.json.type.SizeUnitTypeAdapter;
import com.cleversafe.og.json.type.TimeUnitTypeAdapter;
import com.cleversafe.og.object.ObjectManager;
import com.cleversafe.og.statistic.Statistics;
import com.cleversafe.og.test.LoadTest;
import com.cleversafe.og.util.SizeUnit;
import com.cleversafe.og.util.Version;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;

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
      if (cli.help())
        cli.printUsage();
      else if (cli.version())
        cli.printVersion();
      else if (cli.error()) {
        cli.printErrors();
        cli.printUsage();
        Application.exit(Application.TEST_ERROR);
      }
      Application.exit(0);
    }

    final Gson gson = createGson();
    final OGConfig ogConfig;
    final Injector injector;
    LoadTest test = null;
    Client client = null;
    ObjectManager objectManager = null;
    Statistics statistics = null;
    boolean success = true;
    final ExecutorService executorService = Executors.newSingleThreadExecutor();
    final CompletionService<Boolean> completionService =
        new ExecutorCompletionService<Boolean>(executorService);
    long timestampStart = 0;
    long timestampFinish = 0;

    logBanner();

    try {
      _consoleLogger.info("Configuring...");

      File json = cli.flags().getFile("og_config");
      File defaultJson = new File(Application.getResource("og.json"));
      ogConfig = Application.fromJson(json, defaultJson, OGConfig.class, gson);
      _ogJsonLogger.info(gson.toJson(ogConfig));

      injector = createInjector(ogConfig);
      test = injector.getInstance(LoadTest.class);
      client = injector.getInstance(Client.class);
      objectManager = injector.getInstance(ObjectManager.class);
      statistics = injector.getInstance(Statistics.class);
      Runtime.getRuntime().addShutdownHook(new ShutdownHook(Thread.currentThread()));

      _logger.info("{}", test);
      _consoleLogger.info("Configured.");
      _consoleLogger.info("Test Running...");

      timestampStart = System.currentTimeMillis();
      completionService.submit(test);
      success = completionService.take().get();
      timestampFinish = System.currentTimeMillis();

      if (success)
        _consoleLogger.info("Test Completed.");
      else
        _consoleLogger.error("Test ended unsuccessfully. See og.log for details");

    } catch (InterruptedException e) {
      timestampFinish = System.currentTimeMillis();
      _consoleLogger.info("Test interrupted.");
    } catch (Exception e) {
      timestampFinish = System.currentTimeMillis();
      success = false;

      _logger.error("Exception while configuring and running test", e);
      _consoleLogger.error("Test Error. See og.log for details");
    } finally {
      if (test != null)
        test.stopTest();
      shutdownClient(client);
      MoreExecutors.shutdownAndAwaitTermination(executorService, 1, TimeUnit.MINUTES);
      shutdownObjectManager(objectManager);

      if (statistics != null) {
        final Summary summary = new Summary(statistics, timestampStart, timestampFinish);
        _summaryJsonLogger.info(gson.toJson(summary.getSummaryStats()));

        if (success) {
          logSummaryBanner();
          _consoleLogger.info("{}", summary);
        }
      }
    }

    if (!success)
      Application.exit(Application.TEST_ERROR);
  }

  public static Gson createGson() {
    return new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapterFactory(new CaseInsensitiveEnumTypeAdapterFactory())
        // SizeUnit and TimeUnit TypeAdapters must be registered after
        // CaseInsensitiveEnumTypeAdapterFactory in order to override the registration
        .registerTypeHierarchyAdapter(SizeUnit.class, new SizeUnitTypeAdapter().nullSafe())
        .registerTypeHierarchyAdapter(TimeUnit.class, new TimeUnitTypeAdapter().nullSafe())
        .registerTypeAdapterFactory(new OperationConfigTypeAdapterFactory())
        .registerTypeAdapterFactory(new HostConfigTypeAdapterFactory())
        .registerTypeAdapterFactory(new HostConfigListTypeAdapterFactory())
        .registerTypeAdapterFactory(new SelectionConfigTypeAdapterFactory())
        .registerTypeAdapterFactory(new FilesizeConfigTypeAdapterFactory())
        .registerTypeAdapterFactory(new FilesizeConfigListTypeAdapterFactory())
        .registerTypeAdapterFactory(new ConcurrencyConfigTypeAdapterFactory()).setPrettyPrinting()
        .create();
  }

  public static Injector createInjector(final OGConfig ogConfig) {
    return Guice.createInjector(Stage.PRODUCTION, new OGModule(ogConfig));
  }

  public static void shutdownClient(Client client) {
    try {
      if (client != null)
        Uninterruptibles.getUninterruptibly(client.shutdown(true));
    } catch (final Exception e) {
      _logger.error("Exception while attempting to shutdown client", e);
    }
  }

  public static void shutdownObjectManager(ObjectManager objectManager) {
    try {
      if (objectManager != null)
        objectManager.testComplete();
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
    private final Thread mainThread;

    public ShutdownHook(final Thread mainThread) {
      this.mainThread = mainThread;
    }

    @Override
    public void run() {
      this.mainThread.interrupt();
      Uninterruptibles.joinUninterruptibly(this.mainThread, 1, TimeUnit.MINUTES);
    }
  }
}
