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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.cleversafe.og.http.HttpRequest;
import com.cleversafe.og.operation.Entity;
import com.cleversafe.og.operation.Metadata;
import com.cleversafe.og.operation.Method;
import com.cleversafe.og.operation.Request;
import com.cleversafe.og.util.Pair;
import com.cleversafe.og.util.producer.CachingProducer;
import com.cleversafe.og.util.producer.Producer;
import com.cleversafe.og.util.producer.Producers;

public class RequestProducer implements Producer<Request>
{
   private final Producer<Long> id;
   private final Producer<Method> method;
   private final Producer<URI> uri;
   private final CachingProducer<String> object;
   private final List<Producer<Pair<String, String>>> headers;
   private final Producer<Entity> entity;
   private final Map<String, String> metadata;
   private final Producer<String> username;
   private final Producer<String> password;

   private RequestProducer(
         final Producer<Long> id,
         final Producer<Method> method,
         final Producer<URI> uri,
         final CachingProducer<String> object,
         final List<Producer<Pair<String, String>>> headers,
         final Producer<Entity> entity,
         final Map<String, String> metadata,
         final Producer<String> username,
         final Producer<String> password)
   {
      this.id = checkNotNull(id);
      this.method = checkNotNull(method);
      this.uri = checkNotNull(uri);
      this.object = object;
      this.headers = checkNotNull(headers);
      this.entity = entity;
      this.metadata = checkNotNull(metadata);
      checkArgument((username != null && password != null)
            || (username == null && password == null),
            "username and password must both be either null or not null");
      this.username = username;
      this.password = password;
   }

   @Override
   public Request produce()
   {
      final HttpRequest.Builder context = HttpRequest.custom();
      context.withId(this.id.produce())
            .withMethod(this.method.produce())
            .withUri(this.uri.produce());

      if (this.object != null)
         context.withMetadata(Metadata.OBJECT_NAME, this.object.getCachedValue());

      for (final Producer<Pair<String, String>> producer : this.headers)
      {
         final Pair<String, String> pair = producer.produce();
         context.withHeader(pair.getKey(), pair.getValue());
      }

      if (this.entity != null)
         context.withEntity(this.entity.produce());

      for (final Entry<String, String> e : this.metadata.entrySet())
      {
         context.withMetadata(e.getKey(), e.getValue());
      }

      if (this.username != null)
         context.withMetadata(Metadata.USERNAME, this.username.produce());

      if (this.password != null)
         context.withMetadata(Metadata.PASSWORD, this.password.produce());

      return context.build();
   }

   public static Builder custom()
   {
      return new Builder();
   }

   public static class Builder
   {
      private Producer<Long> id;
      private Producer<Method> method;
      private Producer<URI> uri;
      private CachingProducer<String> object;
      private final List<Producer<Pair<String, String>>> headers;
      private Producer<Entity> entity;
      private final Map<String, String> metadata;
      private Producer<String> username;
      private Producer<String> password;

      private Builder()
      {
         this.headers = new ArrayList<Producer<Pair<String, String>>>();
         this.metadata = new LinkedHashMap<String, String>();
      }

      public Builder withId(final long id)
      {
         checkArgument(id >= 0, "id must be >= 0 [%s]", id);
         return withId(Producers.of(id));
      }

      public Builder withId(final Producer<Long> id)
      {
         this.id = id;
         return this;
      }

      public Builder withMethod(final Method method)
      {
         return withMethod(Producers.of(method));
      }

      public Builder withMethod(final Producer<Method> method)
      {
         this.method = method;
         return this;
      }

      public Builder withUri(final URI uri)
      {
         return withUri(Producers.of(uri));
      }

      public Builder withUri(final Producer<URI> uri)
      {
         this.uri = uri;
         return this;
      }

      public Builder withObject(final CachingProducer<String> object)
      {
         this.object = checkNotNull(object);
         return this;
      }

      public Builder withHeader(final String key, final String value)
      {
         return withHeader(new Pair<String, String>(key, value));
      }

      public Builder withHeader(final Pair<String, String> header)
      {
         return withHeader(Producers.of(header));
      }

      public Builder withHeader(final Producer<Pair<String, String>> header)
      {
         checkNotNull(header);
         this.headers.add(header);
         return this;
      }

      public Builder withEntity(final Entity entity)
      {
         return withEntity(Producers.of(entity));
      }

      public Builder withEntity(final Producer<Entity> entity)
      {
         this.entity = checkNotNull(entity);
         return this;
      }

      public Builder withMetadata(final Metadata key, final String value)
      {
         return withMetadata(key.toString(), value);
      }

      public Builder withMetadata(final String key, final String value)
      {
         this.metadata.put(checkNotNull(key), checkNotNull(value));
         return this;
      }

      public Builder withUsername(final String username)
      {
         return withUsername(Producers.of(username));
      }

      public Builder withUsername(final Producer<String> username)
      {
         this.username = checkNotNull(username);
         return this;
      }

      public Builder withPassword(final String password)
      {
         return withPassword(Producers.of(password));
      }

      public Builder withPassword(final Producer<String> password)
      {
         this.password = checkNotNull(password);
         return this;
      }

      public RequestProducer build()
      {
         return new RequestProducer(this.id, this.method, this.uri, this.object, this.headers,
               this.entity, this.metadata, this.username, this.password);
      }
   }
}
