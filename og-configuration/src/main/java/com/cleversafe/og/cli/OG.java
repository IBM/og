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

import java.util.Locale;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.cli.report.Summary;
import com.cleversafe.og.client.Client;
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
import com.cleversafe.og.object.manager.ObjectManager;
import com.cleversafe.og.object.manager.ObjectManagerException;
import com.cleversafe.og.statistic.Statistics;
import com.cleversafe.og.test.LoadTest;
import com.cleversafe.og.util.SizeUnit;
import com.cleversafe.og.util.Version;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.martiansoftware.jsap.JSAPResult;

public class OG extends AbstractCLI
{
   private static final Logger _logger = LoggerFactory.getLogger(OG.class);
   private static final Logger _testJsonLogger = LoggerFactory.getLogger("TestJsonLogger");
   private static final Logger _clientJsonLogger = LoggerFactory.getLogger("ClientJsonLogger");
   private static final Logger _summaryJsonLogger = LoggerFactory.getLogger("SummaryJsonLogger");
   private static final String JSAP_RESOURCE_NAME = "og.jsap";
   private static final String TEST_JSON = "test.json";
   private static final String CLIENT_JSON = "client.json";
   private static final String LINE_SEPARATOR =
         "-------------------------------------------------------------------------------";

   public static void main(final String[] args)
   {
      final JSAPResult jsapResult = processArgs(JSAP_RESOURCE_NAME, args);

      logBanner();
      _consoleLogger.info("Configuring...");
      final Gson gson = createGson();
      final TestConfig testConfig =
            fromJson(gson, TestConfig.class, jsapResult.getFile("test_config"), TEST_JSON);
      final ClientConfig clientConfig =
            fromJson(gson, ClientConfig.class, jsapResult.getFile("client_config"), CLIENT_JSON);

      _testJsonLogger.info(gson.toJson(testConfig));
      _clientJsonLogger.info(gson.toJson(clientConfig));

      ObjectManager objectManager = null;
      Client client = null;
      try
      {
         final Injector injector = createInjector(testConfig, clientConfig);
         final Statistics stats = injector.getInstance(Statistics.class);
         objectManager = injector.getInstance(ObjectManager.class);
         final LoadTest test = injector.getInstance(LoadTest.class);
         client = injector.getInstance(Client.class);
         _logger.info("{}", test);
         _logger.info("{}", objectManager);

         final ExecutorService executorService = Executors.newSingleThreadExecutor();
         final CompletionService<Boolean> completionService =
               new ExecutorCompletionService<Boolean>(executorService);
         Runtime.getRuntime().addShutdownHook(new ShutdownHook(Thread.currentThread()));
         _consoleLogger.info("Configured.");
         _consoleLogger.info("Test Running...");
         final long timestampStart = System.currentTimeMillis();
         long timestampFinish;
         boolean success = false;
         try
         {
            completionService.submit(test);
            success = completionService.take().get();
         }
         catch (final InterruptedException e)
         {
            _logger.warn("", e);
         }
         catch (final ExecutionException e)
         {
            _logger.error("", e);
         }
         finally
         {
            test.stopTest();
            timestampFinish = System.currentTimeMillis();
            shutdownClient(client, false);
            MoreExecutors.shutdownAndAwaitTermination(executorService, 1, TimeUnit.MINUTES);
            shutdownObjectManager(objectManager);
         }

         if (success)
            _consoleLogger.info("Test Completed.");
         else
            _consoleLogger.error("Test ended abruptly. Check application log for details");

         logSummaryBanner();
         final Summary summary = new Summary(stats, timestampStart, timestampFinish);
         _consoleLogger.info("{}", summary);
         _summaryJsonLogger.info(gson.toJson(summary.getSummaryStats()));

      }
      catch (final Exception e)
      {
         shutdownClient(client, true);
         shutdownObjectManager(objectManager);
         _consoleLogger.error("Error provisioning dependencies. Check application log for details");
         _logger.error("", e);
         System.exit(CONFIGURATION_ERROR);
      }
   }

   private static void logBanner()
   {
      final String bannerFormat = "%s\nObject Generator (%s)\n%s";
      final String banner = String.format(Locale.US, bannerFormat,
            LINE_SEPARATOR, Version.displayVersion(), LINE_SEPARATOR);
      _consoleLogger.info(banner);
   }

   private static void logSummaryBanner()
   {
      final String bannerFormat = "%s\nSummary\n%s";
      final String banner = String.format(Locale.US, bannerFormat, LINE_SEPARATOR, LINE_SEPARATOR);
      _consoleLogger.info(banner);
   }

   private static Injector createInjector(
         final TestConfig testConfig,
         final ClientConfig clientConfig)
   {
      return Guice.createInjector(Stage.PRODUCTION,
            new TestModule(testConfig),
            new ClientModule(clientConfig),
            new ApiModule(),
            new OGModule());
   }

   private static Gson createGson()
   {
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
            .registerTypeAdapterFactory(new ConcurrencyConfigTypeAdapterFactory())
            .setPrettyPrinting()
            .create();
   }

   private static void shutdownObjectManager(final ObjectManager objectManager)
   {
      if (objectManager != null)
      {
         try
         {
            objectManager.testComplete();
         }
         catch (final ObjectManagerException e)
         {
            _consoleLogger.error("Error shutting down object manager", e);
         }
      }
   }

   private static void shutdownClient(final Client client, final boolean immediate)
   {
      if (client != null)
      {
         final ListenableFuture<Boolean> complete = client.shutdown(immediate);
         try
         {
            Uninterruptibles.getUninterruptibly(complete);
         }
         catch (final ExecutionException e)
         {
            _logger.error("Exception while waiting for client shutdown completion", e);
         }
      }
   }

   private static class ShutdownHook extends Thread
   {
      private final Thread mainThread;

      public ShutdownHook(final Thread mainThread)
      {
         this.mainThread = mainThread;
      }

      @Override
      public void run()
      {
         this.mainThread.interrupt();
         Uninterruptibles.joinUninterruptibly(this.mainThread, 1, TimeUnit.MINUTES);
      }
   }
}
