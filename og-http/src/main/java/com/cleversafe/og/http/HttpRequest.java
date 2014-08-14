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
// Date: Mar 11, 2014
// ---------------------

package com.cleversafe.og.http;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.URI;
import java.util.Locale;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.api.Metadata;
import com.cleversafe.og.api.Method;
import com.cleversafe.og.api.Request;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * A defacto implementation of the {@code Request} interface
 * 
 * @since 1.0
 */
public class HttpRequest implements Request
{
   private final Method method;
   private final URI uri;
   private final Map<String, String> headers;
   private final Body body;
   private final Map<String, String> metadata;
   private static final DateTimeFormatter RFC1123 =
         DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss zzz").withLocale(Locale.US);

   private HttpRequest(final Builder builder)
   {
      this.method = checkNotNull(builder.method);
      this.uri = checkNotNull(builder.uri);
      this.headers = ImmutableMap.copyOf(builder.headers);
      this.body = checkNotNull(builder.body);
      this.metadata = ImmutableMap.copyOf(builder.metadata);
   }

   @Override
   public Method getMethod()
   {
      return this.method;
   }

   @Override
   public URI getUri()
   {
      return this.uri;
   }

   @Override
   public Map<String, String> headers()
   {
      return this.headers;
   }

   @Override
   public Body getBody()
   {
      return this.body;
   }

   @Override
   public Map<String, String> metadata()
   {
      return this.metadata;
   }

   /**
    * An http request builder
    */
   public static class Builder
   {
      private final Method method;
      private final URI uri;
      private final Map<String, String> headers;
      private Body body;
      private final Map<String, String> metadata;

      /**
       * Constructs a builder
       * <p>
       * Note: this builder automatically includes a {@code Date} header with an rfc1123 formatted
       * datetime set to the time of builder construction
       * 
       * @param method
       *           the request method for this request
       * @param uri
       *           the uri for this request
       */
      public Builder(final Method method, final URI uri)
      {
         this.method = method;
         this.uri = uri;
         this.headers = Maps.newLinkedHashMap();
         this.headers.put("Date", RFC1123.print(new DateTime()));
         this.body = Bodies.none();
         this.metadata = Maps.newLinkedHashMap();
      }

      /**
       * Configures a request header to include with this request
       * 
       * @param key
       *           a header key
       * @param value
       *           a header value
       * @return this builder
       */
      public Builder withHeader(final String key, final String value)
      {
         this.headers.put(key, value);
         return this;
      }

      /**
       * Configures a request body to include with this request
       * 
       * @param body
       *           a body
       * @return this builder
       */
      public Builder withBody(final Body body)
      {
         this.body = body;
         return this;
      }

      /**
       * Configures an additional piece of metadata to include with this request, using a
       * {@code Metadata} entry as the key
       * 
       * @param key
       *           a metadata key
       * @param value
       *           a metadata value
       * @return this builder
       */
      public Builder withMetadata(final Metadata key, final String value)
      {
         return withMetadata(key.toString(), value);
      }

      /**
       * Configures an additional piece of metadata to include with this request
       * 
       * @param key
       *           a metadata key
       * @param value
       *           a metadata value
       * @return this builder
       */
      public Builder withMetadata(final String key, final String value)
      {
         this.metadata.put(key, value);
         return this;
      }

      /**
       * Constructs an http request instance
       * 
       * @return an http request instance
       * @throws NullPointerException
       *            if any null header or metadata keys or values were added to this builder
       */
      public HttpRequest build()
      {
         return new HttpRequest(this);
      }
   }
}
