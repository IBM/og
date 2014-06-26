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
import com.cleversafe.og.guice.ClientModule;
import com.cleversafe.og.guice.JsonModule;
import com.cleversafe.og.guice.NOHModule;
import com.cleversafe.og.guice.OGModule;
import com.cleversafe.og.guice.ObjectManagerModule;
import com.cleversafe.og.guice.OperationManagerModule;
import com.cleversafe.og.guice.SOHModule;
import com.cleversafe.og.object.manager.ObjectManager;
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
   private static Logger _logger = LoggerFactory.getLogger(OG.class);
   private static Logger _configJsonLogger = LoggerFactory.getLogger("ConfigJsonLogger");
   private static Logger _summaryJsonLogger = LoggerFactory.getLogger("SummaryJsonLogger");
   private static final String JSAP_RESOURCE_NAME = "og.jsap";
   private static final String CONFIG_JSON = "config.json";

   public static void main(final String[] args)
   {
      final JSAPResult jsapResult = processArgs(JSAP_RESOURCE_NAME, args);
      final Gson gson = createGson();
      final JsonConfig config =
            fromJson(gson, JsonConfig.class, jsapResult.getFile("config"), CONFIG_JSON);

      _configJsonLogger.info(gson.toJson(config));

      ObjectManager objectManager = null;
      LoadTest test = null;
      AbstractModule apiModule;
      // TODO better way to do this?
      switch (config.getApi())
      {
         case SOH :
            apiModule = new SOHModule();
            break;
         default :
            apiModule = new NOHModule();
            break;
      }
      try
      {
         final Injector injector =
               Guice.createInjector(new JsonModule(config), new OGModule(),
                     new OperationManagerModule(), apiModule, new ObjectManagerModule(),
                     new ClientModule());
         final Statistics stats = injector.getInstance(Statistics.class);
         objectManager = injector.getInstance(ObjectManager.class);
         test = injector.getInstance(LoadTest.class);
         Runtime.getRuntime().addShutdownHook(
               new ShutdownHook(gson, new Summary(stats), objectManager));
         _consoleLogger.info("running test");
         test.runTest();
         // FIXME this call should be unnecessary as testComplete is already called in the shutdown
         // hook, but may be due to buggy implementation in RandomObjectPopulator join call
         objectManager.testComplete();
      }
      catch (final RuntimeException e)
      {
         _consoleLogger.error("Error provisioning dependencies", e);
         System.exit(CONFIGURATION_ERROR);
      }
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
      private final Gson gson;
      private final Summary summary;
      private final ObjectManager objectManager;

      public ShutdownHook(final Gson gson, final Summary summary, final ObjectManager objectManager)
      {
         this.gson = checkNotNull(gson);
         this.summary = checkNotNull(summary);
         this.objectManager = checkNotNull(objectManager);
      }

      @Override
      public void run()
      {
         _consoleLogger.info("shutting down");
         try
         {
            this.objectManager.testComplete();
         }
         catch (final Exception e)
         {
            _consoleLogger.error("Error shutting down object manager", e);
         }

         _consoleLogger.info("{}", this.summary);
         _summaryJsonLogger.info(this.gson.toJson(this.summary.getSummaryStats()));
      }
   }
}
