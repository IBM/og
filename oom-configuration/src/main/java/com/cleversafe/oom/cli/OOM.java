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

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.oom.cli.json.JSONConfiguration;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;

public class OOM
{
   private static Logger _logger = LoggerFactory.getLogger(OOM.class);
   private static String TEST_JSON_RESOURCE_NAME = "test.json";
   public static int ERROR_MISSING_RESOURCE = 1;
   public static int ERROR_PARSE_EXCEPTION = 2;

   public static void main(final String[] args)
   {
      final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setLongSerializationPolicy(LongSerializationPolicy.STRING)
            .setPrettyPrinting()
            .create();
      final JSONConfiguration config = createJSONConfiguration(gson);
      _logger.info(gson.toJson(config));
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
         System.exit(ERROR_PARSE_EXCEPTION);
      }
      return config;
   }

   private static URL createConfigURL(final String resourceName)
   {
      final URL configURL = ClassLoader.getSystemResource(resourceName);
      if (configURL == null)
      {
         _logger.error("Could not find configuration file on classpath [{}]", resourceName);
         System.exit(ERROR_MISSING_RESOURCE);
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
         System.exit(ERROR_MISSING_RESOURCE);
      }
      return configReader;
   }
}
