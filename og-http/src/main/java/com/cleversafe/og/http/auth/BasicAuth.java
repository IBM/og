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
// Date: Jun 16, 2014
// ---------------------

package com.cleversafe.og.http.auth;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.operation.Metadata;
import com.cleversafe.og.operation.Request;
import com.google.common.io.BaseEncoding;

/**
 * An http auth implementation that creates authorization header values using the basic auth
 * algorithm
 * 
 * @since 1.0
 */
public class BasicAuth implements HttpAuth
{
   private static final Logger _logger = LoggerFactory.getLogger(BasicAuth.class);

   public BasicAuth()
   {}

   @Override
   public String nextAuthorizationHeader(final Request request)
   {
      final String username = request.getMetadata(Metadata.USERNAME);
      final String password = request.getMetadata(Metadata.PASSWORD);
      final String credentials = username + ":" + password;
      final String b64 = BaseEncoding.base64().encode(credentials.getBytes(StandardCharsets.UTF_8));
      return "Basic " + b64;
   }
}
