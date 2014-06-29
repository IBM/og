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
// Date: Jun 20, 2014
// ---------------------

package com.cleversafe.og.util;

import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Version
{
   private static final Logger _logger = LoggerFactory.getLogger(Version.class);
   private static String DISPLAY_VERSION = ResourceBundle.getBundle("og")
         .getString("display.version");

   private Version()
   {}

   public static String displayVersion()
   {
      return DISPLAY_VERSION;
   }
}
