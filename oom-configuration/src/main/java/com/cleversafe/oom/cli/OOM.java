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
import java.io.FileReader;
import java.io.Reader;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.oom.api.OperationManager;
import com.cleversafe.oom.cli.json.JSONConfiguration;
import com.cleversafe.oom.client.Client;
import com.cleversafe.oom.guice.OOMModule;
import com.cleversafe.oom.test.LoadTest;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class OOM
{
   private static Logger _logger = LoggerFactory.getLogger(OOM.class);
   private static String TEST_JSON_RESOURCE_NAME = "test.json";
   public static int ERROR_CONFIGURATION = 1;

   public static void main(final String[] args)
   {
      final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setLongSerializationPolicy(LongSerializationPolicy.STRING)
            .setPrettyPrinting()
            .create();
      final JSONConfiguration config = createJSONConfiguration(gson);
      verifyJSONConfiguration(config);
      _logger.info(gson.toJson(config));
      final Injector injector = Guice.createInjector(new OOMModule(config));
      final OperationManager operationManager = injector.getInstance(OperationManager.class);
      final Client client = injector.getInstance(Client.class);
      final ExecutorService executorService = Executors.newCachedThreadPool();
      final LoadTest test = new LoadTest(operationManager, client, executorService);
      test.runTest();
   }

   private static JSONConfiguration createJSONConfiguration(final Gson gson)
   {
      final URL configURL = createConfigURL(TEST_JSON_RESOURCE_NAME);
      final Reader configReader = createConfigReader(configURL);
      JSONConfiguration config = null;
      try
      {
         config = gson.fromJson(configReader, JSONConfiguration.class);
      }
      catch (final Exception e)
      {
         _logger.error("", e);
         System.exit(ERROR_CONFIGURATION);
      }
      return config;
   }

   private static URL createConfigURL(final String resourceName)
   {
      final URL configURL = ClassLoader.getSystemResource(resourceName);
      if (configURL == null)
      {
         _logger.error("Could not find configuration file on classpath [{}]", resourceName);
         System.exit(ERROR_CONFIGURATION);
      }
      return configURL;
   }

   private static Reader createConfigReader(final URL configURL)
   {
      Reader configReader = null;
      try
      {
         configReader = new FileReader(new File(configURL.toURI()));
      }
      catch (final Exception e)
      {
         _logger.error("", e);
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
}
