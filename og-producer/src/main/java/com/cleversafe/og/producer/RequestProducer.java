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

package com.cleversafe.og.producer;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.cleversafe.og.http.HttpRequest;
import com.cleversafe.og.operation.Entity;
import com.cleversafe.og.operation.Metadata;
import com.cleversafe.og.operation.Method;
import com.cleversafe.og.operation.Request;
import com.cleversafe.og.util.Pair;
import com.google.common.collect.ImmutableList;

/**
 * A producer of requests
 * 
 * @since 1.0
 */
public class RequestProducer implements Producer<Request>
{
   private final Producer<Method> method;
   private final Producer<URI> uri;
   private final List<Pair<Producer<String>, Producer<String>>> headers;
   private final Producer<Entity> entity;
   private final List<Pair<Producer<String>, Producer<String>>> metadata;

   private RequestProducer(final Builder builder)
   {
      this.method = checkNotNull(builder.method);
      this.uri = checkNotNull(builder.uri);
      this.headers = ImmutableList.copyOf(builder.headers);
      this.entity = builder.entity;
      this.metadata = ImmutableList.copyOf(builder.metadata);
   }

   @Override
   public Request produce()
   {
      final HttpRequest.Builder context =
            new HttpRequest.Builder(this.method.produce(), this.uri.produce());

      for (final Pair<Producer<String>, Producer<String>> header : this.headers)
      {
         context.withHeader(header.getKey().produce(), header.getValue().produce());
      }

      if (this.entity != null)
         context.withEntity(this.entity.produce());

      for (final Pair<Producer<String>, Producer<String>> m : this.metadata)
      {
         context.withMetadata(m.getKey().produce(), m.getValue().produce());
      }

      return context.build();
   }

   /**
    * A request producer builder
    */
   public static class Builder
   {
      private final Producer<Method> method;
      private final Producer<URI> uri;
      private final List<Pair<Producer<String>, Producer<String>>> headers;
      private Producer<Entity> entity;
      private final List<Pair<Producer<String>, Producer<String>>> metadata;

      /**
       * Constructs a builder instance using the provided method and uri
       * 
       * @param method
       *           the request method
       * @param uri
       *           the request uri
       */
      public Builder(final Method method, final URI uri)
      {
         this(Producers.of(method), Producers.of(uri));
      }

      /**
       * Constructs a builder instance using the provided method and uri producers
       * 
       * @param method
       *           a request method producer
       * @param uri
       *           a request uri producer
       */
      public Builder(final Producer<Method> method, final Producer<URI> uri)
      {
         this.method = method;
         this.uri = uri;
         this.headers = new ArrayList<Pair<Producer<String>, Producer<String>>>();
         this.metadata = new ArrayList<Pair<Producer<String>, Producer<String>>>();
      }

      /**
       * Configures a request header to include with this request producer
       * 
       * @param key
       *           a header key
       * @param value
       *           a header value
       * @return this builder
       */
      public Builder withHeader(final String key, final String value)
      {
         return withHeader(Producers.of(key), Producers.of(value));
      }

      /**
       * Configures a request header to include with this request producer, using producers for the
       * key and value
       * 
       * @param key
       *           a header key
       * @param value
       *           a header value
       * @return this builder
       */
      public Builder withHeader(final Producer<String> key, final Producer<String> value)
      {
         this.headers.add(new Pair<Producer<String>, Producer<String>>(key, value));
         return this;
      }

      /**
       * Configures a request entity to include with this request producer
       * 
       * @param entity
       *           an entity
       * @return this builder
       */
      public Builder withEntity(final Entity entity)
      {
         return withEntity(Producers.of(entity));
      }

      /**
       * Configures a request entity to include with this request producer, using a producer for the
       * entity
       * 
       * @param entity
       *           an entity
       * @return this builder
       */
      public Builder withEntity(final Producer<Entity> entity)
      {
         this.entity = checkNotNull(entity);
         return this;
      }

      /**
       * Configures an additional piece of metadata to include with this request producer, using a
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
       * Configures an additional piece of metadata to include with this request producer
       * 
       * @param key
       *           a metadata key
       * @param value
       *           a metadata value
       * @return this builder
       */
      public Builder withMetadata(final String key, final String value)
      {
         return withMetadata(Producers.of(key), Producers.of(value));
      }

      /**
       * Configures an additional piece of metadata to include with this request producer, using
       * producers for the key and value
       * 
       * @param key
       *           a metadata key
       * @param value
       *           a metadata value
       * @return this builder
       */
      public Builder withMetadata(final Producer<String> key, final Producer<String> value)
      {
         this.metadata.add(new Pair<Producer<String>, Producer<String>>(key, value));
         return this;
      }

      /**
       * Constructs a request producer instance
       * 
       * @return a request producer instance
       * @throws NullPointerException
       *            if any null header or metadata keys or values were added to this builder
       */
      public RequestProducer build()
      {
         return new RequestProducer(this);
      }
   }

   @Override
   public String toString()
   {
      return String.format(Locale.US,
            "RequestProducer [%nmethod=%s,%nuri=%s,%nheaders=%s,%nentity=%s,%nmetadata=%s%n]",
            this.method,
            this.uri,
            this.headers,
            this.entity,
            this.metadata);
   }
}
