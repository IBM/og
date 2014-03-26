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
// Date: Mar 19, 2014
// ---------------------

package com.cleversafe.oom.http.producer;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import com.cleversafe.oom.api.Producer;
import com.cleversafe.oom.operation.RequestContext;
import com.google.common.base.Joiner;

public class URLProducer implements Producer<URL>
{
   private final Producer<String> scheme;
   private final Producer<String> host;
   private final Producer<Integer> port;
   private final Producer<List<String>> parts;
   private final Producer<Map<String, String>> queryParameters;
   private static final Joiner partJoiner = Joiner.on('/').skipNulls();
   private static final Joiner.MapJoiner paramJoiner = Joiner.on('&').withKeyValueSeparator("=");

   public URLProducer(
         final Producer<String> scheme,
         final Producer<String> host,
         final Producer<Integer> port,
         final Producer<List<String>> parts,
         final Producer<Map<String, String>> queryParameters)
   {
      this.scheme = checkNotNull(scheme, "scheme must not be null");
      this.host = checkNotNull(host, "host must not be null");
      this.port = checkNotNull(port, "port must not be null");
      this.parts = checkNotNull(parts, "parts must not be null");
      this.queryParameters = checkNotNull(queryParameters, "queryParameters must not be null");
   }

   @Override
   public URL produce(final RequestContext context)
   {
      final StringBuilder builder = new StringBuilder()
            .append(this.scheme.produce(context))
            .append("://")
            .append(this.host.produce(context))
            .append(":")
            .append(this.port.produce(context))
            .append("/")
            .append(partJoiner.join(this.parts.produce(context)));

      final String queryParams = paramJoiner.join(this.queryParameters.produce(context));
      if (queryParams.length() > 0)
         builder.append("?").append(queryParams);
      try
      {
         return new URL(builder.toString());
      }
      catch (final MalformedURLException e)
      {
         // TODO fix this
         return null;
      }
   }
}
