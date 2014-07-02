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

import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.operation.Request;
import com.cleversafe.og.util.producer.Producer;
import com.google.common.io.BaseEncoding;

public class BasicAuth implements HttpAuth
{
   private static final Logger _logger = LoggerFactory.getLogger(BasicAuth.class);
   private final Producer<String> username;
   private final Producer<String> password;

   public BasicAuth(final Producer<String> username, final Producer<String> password)
   {
      this.username = checkNotNull(username);
      this.password = checkNotNull(password);
   }

   @Override
   public String nextAuthorizationHeader(final Request request)
   {
      // TODO cache header in common case of constant username and password
      final String user = this.username.produce();
      final String pass = this.password.produce();
      final String credentials = user + ":" + pass;
      final String b64 = BaseEncoding.base64().encode(credentials.getBytes(StandardCharsets.UTF_8));
      return "Basic " + b64;
   }
}
