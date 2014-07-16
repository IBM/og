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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.cli.report.Summary;
import com.cleversafe.og.guice.ApiModule;
import com.cleversafe.og.guice.JsonModule;
import com.cleversafe.og.guice.OGModule;
import com.cleversafe.og.json.JsonConfig;
import com.cleversafe.og.json.type.CaseInsensitiveEnumTypeAdapterFactory;
import com.cleversafe.og.json.type.OperationConfigTypeAdapterFactory;
import com.cleversafe.og.json.type.SizeUnitTypeAdapter;
import com.cleversafe.og.json.type.TimeUnitTypeAdapter;
import com.cleversafe.og.object.manager.ObjectManager;
import com.cleversafe.og.object.manager.ObjectManagerException;
import com.cleversafe.og.statistic.Statistics;
import com.cleversafe.og.test.LoadTest;
import com.cleversafe.og.util.SizeUnit;
import com.cleversafe.og.util.Version;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.ProvisionException;
import com.google.inject.Stage;
import com.martiansoftware.jsap.JSAPResult;

public class OG extends AbstractCLI
{
   private static final Logger _logger = LoggerFactory.getLogger(OG.class);
   private static final Logger _configJsonLogger = LoggerFactory.getLogger("ConfigJsonLogger");
   private static final Logger _summaryJsonLogger = LoggerFactory.getLogger("SummaryJsonLogger");
   private static final String JSAP_RESOURCE_NAME = "og.jsap";
   private static final String CONFIG_JSON = "config.json";
   private static final String LINE_SEPARATOR =
         "-------------------------------------------------------------------------------";

   public static void main(final String[] args)
   {
      final JSAPResult jsapResult = processArgs(JSAP_RESOURCE_NAME, args);

      logBanner();
      _consoleLogger.info("Configuring...");
      final Gson gson = createGson();
      final JsonConfig config =
            fromJson(gson, JsonConfig.class, jsapResult.getFile("config"), CONFIG_JSON);

      _configJsonLogger.info(gson.toJson(config));

      ObjectManager objectManager = null;
      try
      {
         final Injector injector = createInjector(config);
         final Statistics stats = injector.getInstance(Statistics.class);
         objectManager = injector.getInstance(ObjectManager.class);
         final LoadTest test = injector.getInstance(LoadTest.class);
         Runtime.getRuntime().addShutdownHook(new ShutdownHook(test));
         _consoleLogger.info("Configured.");
         _consoleLogger.info("Test Running...");
         final long timestampStart = System.currentTimeMillis();
         final boolean success = test.runTest();
         final long timestampFinish = System.currentTimeMillis();
         shutdownObjectManager(objectManager);

         if (success)
            _consoleLogger.info("Test Completed.");
         else
            _consoleLogger.error("Test ended abruptly. Check application log for details");

         logSummaryBanner();
         final Summary summary = new Summary(stats, timestampStart, timestampFinish);
         _consoleLogger.info("{}", summary);
         _summaryJsonLogger.info(gson.toJson(summary.getSummaryStats()));

      }
      catch (final ProvisionException e)
      {
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

   private static Injector createInjector(final JsonConfig config)
   {
      return Guice.createInjector(Stage.PRODUCTION,
            new JsonModule(config),
            new ApiModule(),
            new OGModule());
   }

   private static Gson createGson()
   {
      return new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setLongSerializationPolicy(LongSerializationPolicy.STRING)
            .registerTypeAdapterFactory(new CaseInsensitiveEnumTypeAdapterFactory())
            // SizeUnit and TimeUnit TypeAdapters must be registered after
            // CaseInsensitiveEnumTypeAdapterFactory in order to override the registration
            .registerTypeAdapter(SizeUnit.class, new SizeUnitTypeAdapter().nullSafe())
            .registerTypeAdapter(TimeUnit.class, new TimeUnitTypeAdapter().nullSafe())
            .registerTypeAdapterFactory(new OperationConfigTypeAdapterFactory())
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

   private static class ShutdownHook extends Thread
   {
      private final LoadTest test;

      public ShutdownHook(final LoadTest test)
      {
         this.test = checkNotNull(test);
      }

      @Override
      public void run()
      {
         this.test.stopTest();
      }
   }
}
