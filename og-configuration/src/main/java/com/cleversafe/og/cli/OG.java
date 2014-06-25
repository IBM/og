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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;

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
import com.cleversafe.og.util.Version;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;

public class OG
{
   private static Logger _logger = LoggerFactory.getLogger(OG.class);
   private static Logger _configJsonLogger = LoggerFactory.getLogger("ConfigJsonLogger");
   private static Logger _summaryJsonLogger = LoggerFactory.getLogger("SummaryJsonLogger");
   private static final String JSAP_RESOURCE_NAME = "og.jsap";
   private static final String CONFIG_JSON = "config.json";
   public static final int NORMAL_TERMINATION = 0;
   public static final int ERROR_CONFIGURATION = 1;

   public static void main(final String[] args)
   {
      final JSAP jsap = getJSAP();
      final JSAPResult jsapResult = jsap.parse(args);
      if (!jsapResult.success())
         printErrorsAndExit(jsap, jsapResult);

      if (jsapResult.getBoolean("version"))
      {
         _logger.info(Version.displayVersion());
         System.exit(NORMAL_TERMINATION);
      }

      if (jsapResult.getBoolean("help"))
      {
         printUsage(jsap);
         System.exit(NORMAL_TERMINATION);
      }

      final File testConfig = getTestConfig(jsapResult);
      _logger.info("configuring test");

      final Gson gson = getGson();
      final JsonConfig config = createJsonConfig(gson, testConfig);
      verifyJsonConfig(config);

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
         _logger.info("running test");
         test.runTest();
         // FIXME this call should be unnecessary as testComplete is already called in the shutdown
         // hook, but may be due to buggy implementation in RandomObjectPopulator join call
         objectManager.testComplete();
      }
      catch (final RuntimeException e)
      {
         _logger.error("Error provisioning dependencies", e);
         System.exit(ERROR_CONFIGURATION);
      }
   }

   private static JSAP getJSAP()
   {
      JSAP jsap = null;
      try
      {
         jsap = new JSAP(getResource(JSAP_RESOURCE_NAME));
      }
      catch (final Exception e)
      {
         _logger.error("Error creating JSAP", e);
         System.exit(ERROR_CONFIGURATION);
      }
      return jsap;
   }

   private static void printErrorsAndExit(final JSAP jsap, final JSAPResult jsapResult)
   {
      @SuppressWarnings("rawtypes")
      final Iterator errs = jsapResult.getErrorMessageIterator();
      while (errs.hasNext())
      {
         _logger.error(errs.next().toString());
      }
      printUsage(jsap);
      System.exit(ERROR_CONFIGURATION);
   }

   private static void printUsage(final JSAP jsap)
   {
      _logger.info("Usage: og " + jsap.getUsage());
      _logger.info(jsap.getHelp());
   }

   private static File getTestConfig(final JSAPResult jsapResult)
   {
      File testConfig = jsapResult.getFile("config");
      if (testConfig == null)
         testConfig = getConfigFile(getResource(CONFIG_JSON));
      return testConfig;
   }

   private static Gson getGson()
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

   private static JsonConfig createJsonConfig(final Gson gson, final File testConfig)
   {
      final Reader configReader = getConfigReader(testConfig);
      JsonConfig config = null;
      try
      {
         config = gson.fromJson(configReader, JsonConfig.class);
      }
      catch (final Exception e)
      {
         _logger.error("Error parsing configuration", e);
         System.exit(ERROR_CONFIGURATION);
      }
      return config;
   }

   private static URL getResource(final String resourceName)
   {
      final URL url = ClassLoader.getSystemResource(resourceName);
      if (url == null)
      {
         _logger.error("Could not find configuration file on classpath [{}]", resourceName);
         System.exit(ERROR_CONFIGURATION);
      }
      return url;
   }

   private static File getConfigFile(final URL resource)
   {
      File configFile = null;
      try
      {
         configFile = new File(resource.toURI());
      }
      catch (final URISyntaxException e)
      {
         _logger.error("Error creating config file", e);
         System.exit(ERROR_CONFIGURATION);
      }
      return configFile;
   }

   private static Reader getConfigReader(final File testConfig)
   {
      Reader configReader = null;
      try
      {
         configReader = new FileReader(testConfig);
      }
      catch (final FileNotFoundException e)
      {
         _logger.error(String.format("Could not find file [%s]", testConfig));
         System.exit(ERROR_CONFIGURATION);
      }
      return configReader;
   }

   private static void verifyJsonConfig(final JsonConfig config)
   {
      try
      {
         checkArgument(config.getHosts().size() > 0, "At least one accesser must be specified");
         checkNotNull(config.getContainer());
      }
      catch (final Exception e)
      {
         _logger.error("", e);
         System.exit(ERROR_CONFIGURATION);
      }
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
         _logger.info("shutting down");
         try
         {
            this.objectManager.testComplete();
         }
         catch (final Exception e)
         {
            _logger.error("Error shutting down object manager", e);
         }

         _logger.info(this.summary.toString());
         _summaryJsonLogger.info(this.gson.toJson(this.summary.getSummaryStats()));
      }
   }
}
