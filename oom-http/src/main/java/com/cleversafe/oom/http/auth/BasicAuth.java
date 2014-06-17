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

package com.cleversafe.oom.http.auth;

import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.oom.api.Producer;
import com.cleversafe.oom.operation.Request;
import com.cleversafe.oom.util.Pair;
import com.google.common.io.BaseEncoding;

public class BasicAuth implements HttpAuth
{
   private static Logger _logger = LoggerFactory.getLogger(BasicAuth.class);
   private final Producer<String> username;
   private final Producer<String> password;

   public BasicAuth(final Producer<String> username, final Producer<String> password)
   {
      this.username = checkNotNull(username, "username must not be null");
      this.password = checkNotNull(password, "password must not be null");
   }

   @Override
   public Pair<String, String> nextAuthorizationHeader(final Request request)
   {
      // TODO cache header in common case of constant username and password
      final String username = this.username.produce(null);
      final String password = this.password.produce(null);
      final String credentials = username + ":" + password;
      final String b64 = BaseEncoding.base64().encode(credentials.getBytes(StandardCharsets.UTF_8));
      return new Pair<String, String>("Authorization", "Basic " + b64);
   }
}
