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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;
import com.martiansoftware.jsap.JSAPResult;

public class ObjectFile extends AbstractCLI
{
   private static final String JSAP_RESOURCE_NAME = "objectfile.jsap";
   private static final BaseEncoding ENCODING = BaseEncoding.base16().lowerCase();
   private static int ID_LENGTH = 18;

   public static void main(final String[] args)
   {
      final JSAPResult jsapResult = processArgs(JSAP_RESOURCE_NAME, args);
      try
      {
         final InputStream in = getInputStream(jsapResult.getFile("input"));
         final OutputStream out = getOutputStream(jsapResult.getFile("output"));
         if (jsapResult.getBoolean("write"))
            write(in, out);
         else if (jsapResult.getBoolean("read"))
            read(in, out);
         else
            identity(in, out);

         if (!out.equals(System.out))
            out.close();
      }
      catch (final IOException e)
      {
         _consoleLogger.error("", e);
         System.exit(UNKNOWN_ERROR);
      }
   }

   public static InputStream getInputStream(final File input) throws FileNotFoundException
   {
      if (input != null)
         return new FileInputStream(input);
      return System.in;
   }

   public static OutputStream getOutputStream(final File output) throws FileNotFoundException
   {
      if (output != null)
         return new FileOutputStream(output);
      return System.out;
   }

   public static void write(final InputStream in, final OutputStream out) throws IOException
   {
      final BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charsets.UTF_8));
      String line;
      while ((line = reader.readLine()) != null)
      {
         out.write(ENCODING.decode(line));
      }
   }

   public static void read(final InputStream in, final OutputStream out) throws IOException
   {
      BufferedWriter writer = null;
      try
      {
         writer = new BufferedWriter(new OutputStreamWriter(out, Charsets.UTF_8));
         final byte[] buf = new byte[ID_LENGTH];
         while (in.read(buf) == ID_LENGTH)
         {
            writer.write(ENCODING.encode(buf));
            writer.newLine();
         }
      }
      finally
      {
         if (writer != null)
            writer.flush();
      }
   }

   public static void identity(final InputStream in, final OutputStream out) throws IOException
   {
      ByteStreams.copy(in, out);
   }
}
