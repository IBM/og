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

package com.cleversafe.og.http.producer;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.cleversafe.og.http.Scheme;
import com.cleversafe.og.util.producer.Producer;
import com.cleversafe.og.util.producer.ProducerException;
import com.cleversafe.og.util.producer.Producers;
import com.google.common.base.Joiner;

public class UriProducer implements Producer<URI>
{
   private final Producer<Scheme> scheme;
   private final Producer<String> host;
   private final Producer<Integer> port;
   private final List<Producer<String>> path;
   private final Map<String, String> queryParameters;
   private final boolean trailingSlash;
   private static final Joiner.MapJoiner PARAM_JOINER = Joiner.on('&').withKeyValueSeparator("=");

   private UriProducer(final Builder builder)
   {
      this.scheme = checkNotNull(builder.scheme);
      this.host = checkNotNull(builder.host);
      this.port = builder.port;
      checkNotNull(builder.path);
      // defensive copy
      this.path = new ArrayList<Producer<String>>();
      for (final Producer<String> p : builder.path)
      {
         this.path.add(checkNotNull(p));
      }
      checkNotNull(builder.queryParameters);
      // defensive copy
      this.queryParameters = new LinkedHashMap<String, String>();
      for (final Entry<String, String> qp : builder.queryParameters.entrySet())
      {
         this.queryParameters.put(checkNotNull(qp.getKey()), checkNotNull(qp.getValue()));
      }
      this.trailingSlash = builder.trailingSlash;
   }

   @Override
   public URI produce()
   {
      final StringBuilder s = new StringBuilder()
            .append(this.scheme.produce())
            .append("://")
            .append(this.host.produce());
      appendPort(s);
      appendPath(s);
      appendTrailingSlash(s);
      appendQueryParams(s);

      try
      {
         return new URI(s.toString());
      }
      catch (final URISyntaxException e)
      {
         // Wrapping checked exception as unchecked because most callers will not be able to handle
         // it and I don't want to include URISyntaxException in the entire signature chain
         throw new ProducerException(e);
      }
   }

   private void appendPort(final StringBuilder s)
   {
      if (this.port != null)
         s.append(":").append(this.port.produce());
   }

   private void appendPath(final StringBuilder s)
   {
      for (final Producer<String> part : this.path)
      {
         s.append("/").append(part.produce());
      }
   }

   private void appendTrailingSlash(final StringBuilder s)
   {
      if (this.trailingSlash)
         s.append("/");
   }

   private void appendQueryParams(final StringBuilder s)
   {
      final String queryParams = PARAM_JOINER.join(this.queryParameters);
      if (queryParams.length() > 0)
         s.append("?").append(queryParams);
   }

   public static class Builder
   {
      private Producer<Scheme> scheme;
      private final Producer<String> host;
      private Producer<Integer> port;
      private final List<Producer<String>> path;
      private final Map<String, String> queryParameters;
      private boolean trailingSlash;

      public Builder(final String host, final List<Producer<String>> path)
      {
         this(Producers.of(host), path);
      }

      public Builder(final Producer<String> host, final List<Producer<String>> path)
      {
         this.scheme = Producers.of(Scheme.HTTP);
         this.host = host;
         this.path = path;
         this.queryParameters = new LinkedHashMap<String, String>();
      }

      public Builder withScheme(final Scheme scheme)
      {
         return withScheme(Producers.of(scheme));
      }

      public Builder withScheme(final Producer<Scheme> scheme)
      {
         this.scheme = scheme;
         return this;
      }

      public Builder onPort(final int port)
      {
         checkArgument(port >= 0, "port must be >= 0 [%s]", port);
         return onPort(Producers.of(port));
      }

      public Builder onPort(final Producer<Integer> port)
      {
         this.port = port;
         return this;
      }

      public Builder withQueryParameter(final String key, final String value)
      {
         this.queryParameters.put(key, value);
         return this;
      }

      public Builder withTrailingSlash()
      {
         this.trailingSlash = true;
         return this;
      }

      public UriProducer build()
      {
         return new UriProducer(this);
      }
   }
}
