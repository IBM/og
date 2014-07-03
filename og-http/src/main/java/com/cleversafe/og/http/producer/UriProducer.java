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
   private final List<Producer<String>> parts;
   private final Map<String, String> queryParameters;
   private final boolean trailingSlash;
   private static final Joiner.MapJoiner PARAM_JOINER = Joiner.on('&').withKeyValueSeparator("=");

   private UriProducer(
         final Producer<Scheme> scheme,
         final Producer<String> host,
         final Producer<Integer> port,
         final List<Producer<String>> parts,
         final Map<String, String> queryParameters,
         final boolean trailingSlash)
   {
      this.scheme = checkNotNull(scheme);
      this.host = checkNotNull(host);
      this.port = port;
      checkNotNull(parts);
      // defensive copy
      this.parts = new ArrayList<Producer<String>>();
      this.parts.addAll(parts);
      this.queryParameters = checkNotNull(queryParameters);
      this.trailingSlash = trailingSlash;
   }

   @Override
   public URI produce()
   {
      final StringBuilder builder = new StringBuilder()
            .append(this.scheme.produce())
            .append("://")
            .append(this.host.produce());
      appendPort(builder);
      appendPath(builder);
      appendTrailingSlash(builder);
      appendQueryParams(builder);

      try
      {
         return new URI(builder.toString());
      }
      catch (final URISyntaxException e)
      {
         // Wrapping checked exception as unchecked because most callers will not be able to handle
         // it and I don't want to include URISyntaxException in the entire signature chain
         throw new ProducerException(e);
      }
   }

   private void appendPort(final StringBuilder builder)
   {
      if (this.port != null)
         builder.append(":").append(this.port.produce());
   }

   private void appendPath(final StringBuilder builder)
   {
      for (final Producer<String> part : this.parts)
      {
         builder.append("/").append(part.produce());
      }
   }

   private void appendTrailingSlash(final StringBuilder builder)
   {
      if (this.trailingSlash)
         builder.append("/");
   }

   private void appendQueryParams(final StringBuilder builder)
   {
      final String queryParams = PARAM_JOINER.join(this.queryParameters);
      if (queryParams.length() > 0)
         builder.append("?").append(queryParams);
   }

   public static Builder custom()
   {
      return new Builder();
   }

   public static class Builder
   {
      private Producer<Scheme> scheme;
      private Producer<String> host;
      private Producer<Integer> port;
      private List<Producer<String>> path;
      private final Map<String, String> queryParameters;
      private boolean trailingSlash;

      private Builder()
      {
         this.scheme = Producers.of(Scheme.HTTP);
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

      public Builder toHost(final String host)
      {
         return toHost(Producers.of(host));
      }

      public Builder toHost(final Producer<String> host)
      {
         this.host = host;
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

      public Builder atPath(final List<Producer<String>> path)
      {
         this.path = path;
         return this;
      }

      public Builder withQueryParameter(final String key, final String value)
      {
         checkNotNull(key);
         checkNotNull(value);
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
         return new UriProducer(this.scheme, this.host, this.port, this.path, this.queryParameters,
               this.trailingSlash);
      }
   }
}
