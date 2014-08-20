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

public class ObjectFile extends CLI
{
   private static final BaseEncoding ENCODING = BaseEncoding.base16().lowerCase();
   private static int ID_LENGTH = 18;
   private InputStream in;
   private OutputStream out;

   public ObjectFile(final String[] args)
   {
      super("object-file", "objectfile.jsap", args);
      try
      {
         this.in = getInputStream();
         this.out = getOutputStream();
      }
      catch (final Exception e)
      {
         _consoleLogger.error("", e);
         this.error = true;
         this.exitCode = CONFIGURATION_ERROR;
      }
   }

   @Override
   public boolean start()
   {
      boolean success = true;
      try
      {
         if (this.jsapResult.getBoolean("write"))
            write();
         else if (this.jsapResult.getBoolean("read"))
            read();
         else
            identity();

         if (!this.out.equals(System.out))
            this.out.close();
      }
      catch (final IOException e)
      {
         _consoleLogger.error("", e);
         success = false;
      }

      return success;
   }

   private InputStream getInputStream() throws FileNotFoundException
   {
      final File input = this.jsapResult.getFile("input");
      if (input != null)
         return new FileInputStream(input);
      return System.in;
   }

   private OutputStream getOutputStream() throws FileNotFoundException
   {
      final File output = this.jsapResult.getFile("output");
      if (output != null)
         return new FileOutputStream(output);
      return System.out;
   }

   private void write() throws IOException
   {
      final BufferedReader reader =
            new BufferedReader(new InputStreamReader(this.in, Charsets.UTF_8));
      String line;
      while ((line = reader.readLine()) != null)
      {
         this.out.write(ENCODING.decode(line));
      }
   }

   private void read() throws IOException
   {
      BufferedWriter writer = null;
      try
      {
         writer = new BufferedWriter(new OutputStreamWriter(this.out, Charsets.UTF_8));
         final byte[] buf = new byte[ID_LENGTH];
         while (this.in.read(buf) == ID_LENGTH)
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

   private void identity() throws IOException
   {
      ByteStreams.copy(this.in, this.out);
   }

   public static void main(final String[] args)
   {
      final ObjectFile of = new ObjectFile(args);
      if (of.shouldStop())
      {
         if (of.error())
         {
            of.printErrors();
            of.printUsage();
         }
         else if (of.help())
            of.printUsage();
         else if (of.version())
            of.printVersion();

         of.exit(of.exitCode());
      }

      of.start();
   }
}
