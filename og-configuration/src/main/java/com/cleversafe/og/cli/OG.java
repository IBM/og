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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.cli.json.JsonConfig;
import com.cleversafe.og.cli.json.type.CaseInsensitiveEnumTypeAdapterFactory;
import com.cleversafe.og.cli.json.type.SizeUnitTypeAdapterFactory;
import com.cleversafe.og.cli.json.type.TimeUnitTypeAdapterFactory;
import com.cleversafe.og.cli.report.Summary;
import com.cleversafe.og.guice.ApiModule;
import com.cleversafe.og.guice.ClientModule;
import com.cleversafe.og.guice.JsonModule;
import com.cleversafe.og.guice.OGModule;
import com.cleversafe.og.guice.ObjectManagerModule;
import com.cleversafe.og.guice.OperationManagerModule;
import com.cleversafe.og.guice.SOHModule;
import com.cleversafe.og.http.util.Api;
import com.cleversafe.og.object.manager.ObjectManager;
import com.cleversafe.og.object.manager.ObjectManagerException;
import com.cleversafe.og.statistic.Statistics;
import com.cleversafe.og.test.LoadTest;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.martiansoftware.jsap.JSAPResult;

public class OG extends AbstractCLI
{
   private static final Logger _logger = LoggerFactory.getLogger(OG.class);
   private static final Logger _configJsonLogger = LoggerFactory.getLogger("ConfigJsonLogger");
   private static final Logger _summaryJsonLogger = LoggerFactory.getLogger("SummaryJsonLogger");
   private static final String JSAP_RESOURCE_NAME = "og.jsap";
   private static final String CONFIG_JSON = "config.json";

   public static void main(final String[] args)
   {
      final JSAPResult jsapResult = processArgs(JSAP_RESOURCE_NAME, args);
      final Gson gson = createGson();
      final JsonConfig config =
            fromJson(gson, JsonConfig.class, jsapResult.getFile("config"), CONFIG_JSON);

      _configJsonLogger.info(gson.toJson(config));

      try
      {
         final Injector injector = createInjector(config);
         final Statistics stats = injector.getInstance(Statistics.class);
         final ObjectManager objectManager = injector.getInstance(ObjectManager.class);
         final LoadTest test = injector.getInstance(LoadTest.class);
         Runtime.getRuntime().addShutdownHook(new ShutdownHook(Thread.currentThread(), test));
         _consoleLogger.info("running test");
         final boolean success = test.runTest();
         if (!success)
            _consoleLogger.error("Test ended abruptly. Check application log for details");
         try
         {
            objectManager.testComplete();
         }
         catch (final ObjectManagerException e)
         {
            _consoleLogger.error("Error shutting down object manager", e);
         }

         final Summary summary = new Summary(stats);
         _consoleLogger.info("{}", summary);
         _summaryJsonLogger.info(gson.toJson(summary.getSummaryStats()));
      }
      catch (final RuntimeException e)
      {
         _consoleLogger.error("Error provisioning dependencies", e);
         System.exit(CONFIGURATION_ERROR);
      }
   }

   private static Injector createInjector(final JsonConfig config)
   {
      return Guice.createInjector(
            new JsonModule(config),
            new OGModule(),
            new OperationManagerModule(),
            apiModule(config.getApi()),
            new ObjectManagerModule(),
            new ClientModule());
   }

   private static AbstractModule apiModule(final Api api)
   {
      if (Api.SOH == api)
         return new SOHModule();
      return new ApiModule();
   }

   private static Gson createGson()
   {
      return new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setLongSerializationPolicy(LongSerializationPolicy.STRING)
            .registerTypeAdapterFactory(new CaseInsensitiveEnumTypeAdapterFactory())
            // TODO refactor into an abstract adapter for enums with behavior
            .registerTypeAdapterFactory(new TimeUnitTypeAdapterFactory())
            .registerTypeAdapterFactory(new SizeUnitTypeAdapterFactory())
            .setPrettyPrinting()
            .create();
   }

   private static class ShutdownHook extends Thread
   {
      private final Thread mainThread;
      private final LoadTest test;

      public ShutdownHook(final Thread mainThread, final LoadTest test)
      {
         this.mainThread = checkNotNull(mainThread);
         this.test = checkNotNull(test);
      }

      @Override
      public void run()
      {
         this.test.stopTest();
         this.mainThread.interrupt();
         try
         {
            this.mainThread.join();
         }
         catch (final InterruptedException e)
         {
            _logger.warn("Shutdown hook interrupted while waiting for main thread to terminate", e);
         }
      }
   }
}
