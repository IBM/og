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

package com.cleversafe.oom.cli;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.oom.api.OperationManager;
import com.cleversafe.oom.cli.json.JSONConfiguration;
import com.cleversafe.oom.cli.json.SizeUnitTypeAdapterFactory;
import com.cleversafe.oom.cli.json.TimeUnitTypeAdapterFactory;
import com.cleversafe.oom.client.Client;
import com.cleversafe.oom.guice.OOMModule;
import com.cleversafe.oom.object.manager.ObjectManager;
import com.cleversafe.oom.test.LoadTest;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;

public class OOM
{
   private static Logger _logger = LoggerFactory.getLogger(OOM.class);
   private static Logger _configJsonLogger = LoggerFactory.getLogger("ConfigJsonLogger");
   private static final String JSAP_RESOURCE_NAME = "oom.jsap";
   private static final String TEST_JSON_RESOURCE_NAME = "test.json";
   public static final int NORMAL_TERMINATION = 0;
   public static final int ERROR_CONFIGURATION = 1;

   public static void main(final String[] args)
   {
      final JSAP jsap = getJSAP();
      final JSAPResult jsapResult = jsap.parse(args);
      if (!jsapResult.success())
         printErrorsAndExit(jsap, jsapResult);

      if (jsapResult.getBoolean("help"))
      {
         printUsage(jsap);
         System.exit(NORMAL_TERMINATION);
      }

      final File testConfig = getTestConfig(jsapResult);
      _logger.info("configuring test");

      final Gson gson = getGson();
      final JSONConfiguration config = createJSONConfiguration(gson, testConfig);
      verifyJSONConfiguration(config);

      _configJsonLogger.info(gson.toJson(config));

      OperationManager operationManager = null;
      ObjectManager objectManager = null;
      Client client = null;
      try
      {
         final Injector injector = Guice.createInjector(new OOMModule(config));
         operationManager = injector.getInstance(OperationManager.class);
         client = injector.getInstance(Client.class);
         objectManager = injector.getInstance(ObjectManager.class);
         Runtime.getRuntime().addShutdownHook(new ShutdownHook(objectManager));
      }
      catch (final RuntimeException e)
      {
         _logger.error("Error provisioning dependencies", e);
         System.exit(ERROR_CONFIGURATION);
      }

      final ExecutorService executorService = Executors.newCachedThreadPool();
      final LoadTest test = new LoadTest(operationManager, client, executorService);

      _logger.info("running test");
      test.runTest();
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
         printUsage(jsap);
      }
      System.exit(ERROR_CONFIGURATION);
   }

   private static void printUsage(final JSAP jsap)
   {
      _logger.info("Usage: oom " + jsap.getUsage());
      _logger.info(jsap.getHelp());
   }

   private static File getTestConfig(final JSAPResult jsapResult)
   {
      File testConfig = jsapResult.getFile("config");
      if (testConfig == null)
         testConfig = getConfigFile(getResource(TEST_JSON_RESOURCE_NAME));
      return testConfig;
   }

   private static Gson getGson()
   {
      return new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setLongSerializationPolicy(LongSerializationPolicy.STRING)
            // TODO refactor into an abstract adapter for enums with behavior
            .registerTypeAdapterFactory(new TimeUnitTypeAdapterFactory())
            .registerTypeAdapterFactory(new SizeUnitTypeAdapterFactory())
            .setPrettyPrinting()
            .create();
      // TODO gson is deserializing into null if an enum could not be parsed. Should throw an
      // exception instead? Otherwise verification can take place in a verification method after
      // json deserialization
   }

   private static JSONConfiguration createJSONConfiguration(final Gson gson, final File testConfig)
   {
      final Reader configReader = getConfigReader(testConfig);
      JSONConfiguration config = null;
      try
      {
         config = gson.fromJson(configReader, JSONConfiguration.class);
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

   private static void verifyJSONConfiguration(final JSONConfiguration config)
   {
      try
      {
         checkArgument(config.getHosts().size() > 0, "At least one accesser must be specified");
         checkNotNull(config.getContainer(), "vault must not be null");
      }
      catch (final Exception e)
      {
         _logger.error("", e);
         System.exit(ERROR_CONFIGURATION);
      }
   }

   private static class ShutdownHook extends Thread
   {
      private final ObjectManager objectManager;

      public ShutdownHook(final ObjectManager objectManager)
      {
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
      }
   }
}
