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
// Date: Apr 2, 2014
// ---------------------

package com.cleversafe.oom.http.producer;

import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.charset.StandardCharsets;

import com.cleversafe.oom.api.Producer;
import com.cleversafe.oom.operation.RequestContext;
import com.cleversafe.oom.util.Pair;
import com.google.common.io.BaseEncoding;

public class BasicAuthProducer implements Producer<Pair<String, String>>
{
   Pair<String, String> header;

   public BasicAuthProducer(final String username, final String password)
   {
      checkNotNull(username, "username must not be null");
      checkNotNull(password, "password must not be null");
      final String credentials = username + ":" + password;
      final String b64 = BaseEncoding.base64().encode(credentials.getBytes(StandardCharsets.UTF_8));
      this.header = new Pair<String, String>("Authorization", "Basic " + b64);
   }

   @Override
   public Pair<String, String> produce(final RequestContext context)
   {
      return this.header;
   }

}
