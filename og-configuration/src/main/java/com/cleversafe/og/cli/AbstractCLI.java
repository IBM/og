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
import java.io.FileReader;
import java.io.Reader;
import java.net.URL;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.util.Version;
import com.google.gson.Gson;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;

public abstract class AbstractCLI
{
   private static Logger _logger = LoggerFactory.getLogger(AbstractCLI.class);
   protected static Logger _consoleLogger = LoggerFactory.getLogger("ConsoleLogger");
   protected static final int NORMAL = 0;
   protected static final int CONFIGURATION_ERROR = 1;
   protected static final int UNKNOWN_ERROR = 2;

   protected static JSAPResult processArgs(final String jsapResourceName, final String[] args)
   {
      JSAPResult jsapResult = null;
      try
      {
         final JSAP jsap = new JSAP(getResource(jsapResourceName));
         jsapResult = jsap.parse(args);
         if (!jsapResult.success())
         {
            printErrors(jsapResult);
            printUsage(jsap);
            System.exit(CONFIGURATION_ERROR);
         }

         if (jsapResult.getBoolean("version"))
         {
            _consoleLogger.info(Version.displayVersion());
            System.exit(NORMAL);
         }

         if (jsapResult.getBoolean("help"))
         {
            printUsage(jsap);
            System.exit(NORMAL);
         }
      }
      catch (final Exception e)
      {
         _consoleLogger.error("Error processing cli flags. Check application log for details");
         _logger.error("", e);
         System.exit(CONFIGURATION_ERROR);
      }
      return jsapResult;
   }

   private static void printErrors(final JSAPResult jsapResult)
   {
      @SuppressWarnings("rawtypes")
      final Iterator errs = jsapResult.getErrorMessageIterator();
      while (errs.hasNext())
      {
         _consoleLogger.error("{}", errs.next());
      }
   }

   private static void printUsage(final JSAP jsap)
   {
      _consoleLogger.info("Usage: og {}", jsap.getUsage());
      _consoleLogger.info(jsap.getHelp());
   }

   protected static <T> T fromJson(
         final Gson gson,
         final Class<T> cls,
         final File userConfig,
         final String defaultConfigResource)
   {
      T config = null;
      try
      {
         File json = userConfig;
         if (userConfig == null)
            json = new File(getResource(defaultConfigResource).toURI());
         final Reader reader = new FileReader(json);
         config = gson.fromJson(reader, cls);
      }
      catch (final Exception e)
      {
         _consoleLogger.error("Error processing json configuration. Check application log for details");
         _logger.error("", e);
         System.exit(CONFIGURATION_ERROR);
      }
      return config;
   }

   protected static URL getResource(final String resourceName)
   {
      final URL url = ClassLoader.getSystemResource(resourceName);
      if (url == null)
         throw new RuntimeException(String.format(
               "Could not find configuration file on classpath [%s]", resourceName));
      return url;
   }
}
