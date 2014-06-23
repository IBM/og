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
// Date: Jun 23, 2014
// ---------------------

package com.cleversafe.og.http.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.URI;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;

public class UriUtil
{
   private static Logger _logger = LoggerFactory.getLogger(UriUtil.class);
   private static final Splitter uriSplitter = Splitter.on("/").omitEmptyStrings();

   private UriUtil()
   {}

   public static String getObjectName(final URI uri)
   {
      checkNotNull(uri, "uri must not be null");
      final List<String> parts = uriSplitter.splitToList(uri.getPath());

      if (parts.size() == 3)
         return parts.get(2);

      if (parts.size() == 2)
      {
         try
         {
            // if 2 parts and first part is an api, must be soh write
            ApiType.valueOf(parts.get(0).toUpperCase(Locale.US));
            return null;
         }
         catch (final IllegalArgumentException e)
         {
            return parts.get(1);
         }
      }
      return null;
   }
}
