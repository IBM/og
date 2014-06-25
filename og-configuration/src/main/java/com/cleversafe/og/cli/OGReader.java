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
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.util.Version;
import com.google.common.io.BaseEncoding;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;

public class OGReader
{
   private static Logger _logger = LoggerFactory.getLogger(OGReader.class);
   private static final String JSAP_RESOURCE_NAME = "ogreader.jsap";
   public static final int NORMAL_TERMINATION = 0;
   public static final int ERROR_CONFIGURATION = 1;
   // TODO place this constant in util somewhere?
   private static int ID_LENGTH = 18;

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

      try
      {
         readIdFile(jsapResult.getFile("object_file"));
      }
      catch (final IOException e)
      {
         _logger.error("Exception reading object file:", e);
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
         _logger.error("%s", errs.next());
         printUsage(jsap);
      }
      System.exit(ERROR_CONFIGURATION);
   }

   private static void printUsage(final JSAP jsap)
   {
      _logger.info("Usage: og-reader " + jsap.getUsage());
      _logger.info(jsap.getHelp());
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

   public static void readIdFile(final File filename) throws IOException
   {
      FileInputStream in = null;
      try
      {
         in = new FileInputStream(filename);
         final byte[] objectID = new byte[ID_LENGTH];
         int numRecords = 0;
         while (in.read(objectID, 0, ID_LENGTH) == ID_LENGTH)
         {
            numRecords++;
            final String id = BaseEncoding.base16().lowerCase().encode(objectID);
            _logger.info(id);
         }
         _logger.info(numRecords + " records");
      }
      finally
      {
         if (in != null)
            in.close();
      }
   }
}
