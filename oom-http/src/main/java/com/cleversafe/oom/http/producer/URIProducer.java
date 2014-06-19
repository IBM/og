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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import com.cleversafe.oom.api.Producer;
import com.cleversafe.oom.api.ProducerException;
import com.cleversafe.oom.http.Scheme;
import com.google.common.base.Joiner;

public class URIProducer implements Producer<URI>
{
   private final Producer<Scheme> scheme;
   private final Producer<String> host;
   private final Producer<Integer> port;
   private final List<Producer<String>> parts;
   // TODO this type is awkward
   private final Producer<Map<String, String>> queryParameters;
   private final boolean trailingSlash;
   private static final Joiner.MapJoiner paramJoiner = Joiner.on('&').withKeyValueSeparator("=");

   private URIProducer(
         final Producer<Scheme> scheme,
         final Producer<String> host,
         final Producer<Integer> port,
         final List<Producer<String>> parts,
         final Producer<Map<String, String>> queryParameters,
         final boolean trailingSlash)
   {
      this.scheme = checkNotNull(scheme, "scheme must not be null");
      this.host = checkNotNull(host, "host must not be null");
      this.port = port;
      this.parts = checkNotNull(parts, "parts must not be null");
      this.queryParameters = checkNotNull(queryParameters, "queryParameters must not be null");
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
      final String queryParams = paramJoiner.join(this.queryParameters.produce());
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
      private Producer<Map<String, String>> queryParams;
      private boolean trailingSlash;

      private Builder()
      {}

      public Builder withScheme(final Producer<Scheme> scheme)
      {
         this.scheme = scheme;
         return this;
      }

      public Builder toHost(final Producer<String> host)
      {
         this.host = host;
         return this;
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

      public Builder withQueryParams(final Producer<Map<String, String>> queryParams)
      {
         this.queryParams = queryParams;
         return this;
      }

      public Builder withTrailingSlash()
      {
         this.trailingSlash = true;
         return this;
      }

      public URIProducer build()
      {
         return new URIProducer(this.scheme, this.host, this.port, this.path, this.queryParams,
               this.trailingSlash);
      }
   }
}
