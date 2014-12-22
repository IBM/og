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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.api.Client;
import com.cleversafe.og.guice.ApiModule;
import com.cleversafe.og.guice.ClientModule;
import com.cleversafe.og.guice.OGModule;
import com.cleversafe.og.guice.TestModule;
import com.cleversafe.og.json.ClientConfig;
import com.cleversafe.og.json.TestConfig;
import com.cleversafe.og.json.type.CaseInsensitiveEnumTypeAdapterFactory;
import com.cleversafe.og.json.type.ConcurrencyConfigTypeAdapterFactory;
import com.cleversafe.og.json.type.FilesizeConfigListTypeAdapterFactory;
import com.cleversafe.og.json.type.FilesizeConfigTypeAdapterFactory;
import com.cleversafe.og.json.type.HostConfigListTypeAdapterFactory;
import com.cleversafe.og.json.type.HostConfigTypeAdapterFactory;
import com.cleversafe.og.json.type.OperationConfigTypeAdapterFactory;
import com.cleversafe.og.json.type.SizeUnitTypeAdapter;
import com.cleversafe.og.json.type.TimeUnitTypeAdapter;
import com.cleversafe.og.object.ObjectManager;
import com.cleversafe.og.statistic.Statistics;
import com.cleversafe.og.test.LoadTest;
import com.cleversafe.og.util.SizeUnit;
import com.cleversafe.og.util.Version;
import com.google.common.base.Charsets;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;

public class ObjectGenerator extends CLI {
  private static final Logger _logger = LoggerFactory.getLogger(ObjectGenerator.class);
  private static final Logger _testJsonLogger = LoggerFactory.getLogger("TestJsonLogger");
  private static final Logger _clientJsonLogger = LoggerFactory.getLogger("ClientJsonLogger");
  protected static final Logger _consoleLogger = LoggerFactory.getLogger("ConsoleLogger");
  private static final Logger _summaryJsonLogger = LoggerFactory.getLogger("SummaryJsonLogger");
  private static final String LINE_SEPARATOR =
      "-------------------------------------------------------------------------------";
  private Gson gson;
  private Injector injector;
  private ExecutorService executorService;
  private CompletionService<Boolean> completionService;

  public ObjectGenerator(final String[] args) {
    super("og", "og.jsap", args);
    if (this.error)
      return;

    try {
      this.gson = createGson();
      final TestConfig testConfig =
          fromJson(TestConfig.class, this.jsapResult.getFile("test_config"), "test.json");
      final ClientConfig clientConfig =
          fromJson(ClientConfig.class, this.jsapResult.getFile("client_config"), "client.json");
      _testJsonLogger.info(this.gson.toJson(testConfig));
      _clientJsonLogger.info(this.gson.toJson(clientConfig));
      this.injector = createInjector(testConfig, clientConfig);
      this.executorService = Executors.newSingleThreadExecutor();
      this.completionService = new ExecutorCompletionService<Boolean>(this.executorService);
    } catch (final Exception e) {
      _logger.error("", e);
      this.error = true;
      this.exitCode = CONFIGURATION_ERROR;
    }
  }

  @Override
  public boolean start() {
    final LoadTest test = getInjector().getInstance(LoadTest.class);
    boolean success = false;
    try {
      this.completionService.submit(test);
      success = this.completionService.take().get();
    } catch (final Exception e) {
      _logger.error("", e);
    } finally {
      test.stopTest();
      shutdownClient();
      MoreExecutors.shutdownAndAwaitTermination(this.executorService, 1, TimeUnit.MINUTES);
      shutdownObjectManager();
    }

    return success;
  }

  private Gson createGson() {
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
        .registerTypeAdapterFactory(new FilesizeConfigTypeAdapterFactory())
        .registerTypeAdapterFactory(new FilesizeConfigListTypeAdapterFactory())
        .registerTypeAdapterFactory(new ConcurrencyConfigTypeAdapterFactory()).setPrettyPrinting()
        .create();
  }

  private <T> T fromJson(final Class<T> cls, final File userConfig,
      final String defaultConfigResource) {
    File json = userConfig;
    if (userConfig == null)
      json = new File(getResource(defaultConfigResource));
    Reader reader = null;
    try {
      reader = new InputStreamReader(new FileInputStream(json), Charsets.UTF_8);
    } catch (final FileNotFoundException e) {
      throw new IllegalStateException(e);
    }

    return this.gson.fromJson(reader, cls);
  }

  private Injector createInjector(final TestConfig testConfig, final ClientConfig clientConfig) {
    try {
      return Guice.createInjector(Stage.PRODUCTION, new TestModule(testConfig), new ClientModule(
          clientConfig), new ApiModule(), new OGModule());
    } catch (final Exception e) {
      shutdownClient();
      shutdownObjectManager();
      throw new IllegalStateException(e);
    }
  }

  public Injector getInjector() {
    return this.injector;
  }

  public Gson getGson() {
    return this.gson;
  }

  public void shutdownObjectManager() {
    try {
      final ObjectManager objectManager = this.injector.getInstance(ObjectManager.class);
      if (objectManager != null)
        objectManager.testComplete();
    } catch (final Exception e) {
      _logger.error("Error shutting down object manager", e);
    }
  }

  public void shutdownClient() {
    try {
      final Client client = this.injector.getInstance(Client.class);
      if (client != null)
        Uninterruptibles.getUninterruptibly(client.shutdown(true));
    } catch (final Exception e) {
      _logger.error("Exception while attempting to shutdown client", e);
    }
  }

  public static void main(final String[] args) {
    logBanner();
    _consoleLogger.info("Configuring...");

    final ObjectGenerator og = new ObjectGenerator(args);
    if (og.shouldStop()) {
      if (og.error()) {
        og.printErrors();
        og.printUsage();
      } else if (og.help())
        og.printUsage();
      else if (og.version())
        og.printVersion();

      og.exit(og.exitCode());
    }

    Runtime.getRuntime().addShutdownHook(new ShutdownHook(Thread.currentThread()));
    _consoleLogger.info("Configured.");
    _consoleLogger.info("Test Running...");

    final LoadTest test = og.getInjector().getInstance(LoadTest.class);
    _logger.info("{}", test);

    final long timestampStart = System.currentTimeMillis();
    final boolean success = og.start();
    final long timestampFinish = System.currentTimeMillis();

    if (success)
      _consoleLogger.info("Test Completed.");
    else
      _consoleLogger.error("Test ended abruptly. Check application log for details");

    logSummaryBanner();
    final Statistics stats = og.getInjector().getInstance(Statistics.class);
    final Summary summary = new Summary(stats, timestampStart, timestampFinish);
    _consoleLogger.info("{}", summary);
    _summaryJsonLogger.info(og.getGson().toJson(summary.getSummaryStats()));
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
