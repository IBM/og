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

import java.io.IOException;
import java.net.URL;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;

public class OOM
{
   private static final String JSAP_RESOURCE_NAME = "oom.jsap";

   public static void main(final String[] args) throws IOException, JSAPException
   {
      final URL jsapURL = ClassLoader.getSystemResource(JSAP_RESOURCE_NAME);
      final JSAP jsap = new JSAP(jsapURL);
      System.out.println(jsap.getHelp());
   }
}
