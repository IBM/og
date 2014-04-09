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
// Date: Mar 27, 2014
// ---------------------

package com.cleversafe.oom.guice;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import com.cleversafe.oom.api.Producer;
import com.cleversafe.oom.cli.json.Concurrency;
import com.cleversafe.oom.cli.json.FileSize;
import com.cleversafe.oom.cli.json.JSONConfiguration;
import com.cleversafe.oom.cli.json.OperationConfig;
import com.cleversafe.oom.distribution.Distribution;
import com.cleversafe.oom.distribution.LogNormalDistribution;
import com.cleversafe.oom.distribution.NormalDistribution;
import com.cleversafe.oom.distribution.UniformDistribution;
import com.cleversafe.oom.guice.annotation.DefaultAuth;
import com.cleversafe.oom.guice.annotation.DefaultContainer;
import com.cleversafe.oom.guice.annotation.DefaultEntity;
import com.cleversafe.oom.guice.annotation.DefaultHeaders;
import com.cleversafe.oom.guice.annotation.DefaultHost;
import com.cleversafe.oom.guice.annotation.DefaultId;
import com.cleversafe.oom.guice.annotation.DefaultMetaData;
import com.cleversafe.oom.guice.annotation.DefaultPort;
import com.cleversafe.oom.guice.annotation.DefaultQueryParams;
import com.cleversafe.oom.guice.annotation.DefaultScheme;
import com.cleversafe.oom.guice.annotation.DefaultUrlRoot;
import com.cleversafe.oom.guice.annotation.DeleteAuth;
import com.cleversafe.oom.guice.annotation.DeleteContainer;
import com.cleversafe.oom.guice.annotation.DeleteHeaders;
import com.cleversafe.oom.guice.annotation.DeleteHost;
import com.cleversafe.oom.guice.annotation.DeletePort;
import com.cleversafe.oom.guice.annotation.DeleteQueryParams;
import com.cleversafe.oom.guice.annotation.DeleteScheme;
import com.cleversafe.oom.guice.annotation.ReadAuth;
import com.cleversafe.oom.guice.annotation.ReadContainer;
import com.cleversafe.oom.guice.annotation.ReadHeaders;
import com.cleversafe.oom.guice.annotation.ReadHost;
import com.cleversafe.oom.guice.annotation.ReadPort;
import com.cleversafe.oom.guice.annotation.ReadQueryParams;
import com.cleversafe.oom.guice.annotation.ReadScheme;
import com.cleversafe.oom.guice.annotation.WriteAuth;
import com.cleversafe.oom.guice.annotation.WriteContainer;
import com.cleversafe.oom.guice.annotation.WriteHeaders;
import com.cleversafe.oom.guice.annotation.WriteHost;
import com.cleversafe.oom.guice.annotation.WritePort;
import com.cleversafe.oom.guice.annotation.WriteQueryParams;
import com.cleversafe.oom.guice.annotation.WriteScheme;
import com.cleversafe.oom.http.Scheme;
import com.cleversafe.oom.http.producer.BasicAuthProducer;
import com.cleversafe.oom.operation.Entity;
import com.cleversafe.oom.operation.OperationType;
import com.cleversafe.oom.operation.OperationTypeMix;
import com.cleversafe.oom.operation.RequestContext;
import com.cleversafe.oom.scheduling.RequestRateScheduler;
import com.cleversafe.oom.scheduling.Scheduler;
import com.cleversafe.oom.util.Entities;
import com.cleversafe.oom.util.Pair;
import com.cleversafe.oom.util.WeightedRandomChoice;
import com.cleversafe.oom.util.producer.Producers;
import com.google.common.base.CharMatcher;
import com.google.common.math.DoubleMath;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class JsonModule extends AbstractModule
{
   private final JSONConfiguration config;

   public JsonModule(final JSONConfiguration config)
   {
      this.config = checkNotNull(config, "config must not be null");
   }

   @Override
   protected void configure()
   {}

   @Provides
   JSONConfiguration provideJSONConfiguration()
   {
      return this.config;
   }

   @Provides
   @Singleton
   @DefaultId
   Producer<Long> provideDefaultIdProducer()
   {
      return new Producer<Long>()
      {
         private final AtomicLong id = new AtomicLong();

         @Override
         public Long produce(final RequestContext context)
         {
            return this.id.getAndIncrement();
         }
      };
   }

   @Provides
   @Singleton
   @DefaultScheme
   Producer<Scheme> provideDefaultScheme()
   {
      return Producers.of(this.config.getScheme());
   }

   @Provides
   @Singleton
   @WriteScheme
   Producer<Scheme> provideWriteScheme(@DefaultScheme final Producer<Scheme> scheme)
   {
      return provideScheme(OperationType.WRITE, scheme);
   }

   @Provides
   @Singleton
   @ReadScheme
   Producer<Scheme> provideReadScheme(@DefaultScheme final Producer<Scheme> scheme)
   {
      return provideScheme(OperationType.READ, scheme);
   }

   @Provides
   @Singleton
   @DeleteScheme
   Producer<Scheme> provideDeleteScheme(@DefaultScheme final Producer<Scheme> scheme)
   {
      return provideScheme(OperationType.DELETE, scheme);
   }

   private Producer<Scheme> provideScheme(
         final OperationType operationType,
         final Producer<Scheme> defaultScheme)
   {
      final OperationConfig config = this.config.getOperationConfig().get(operationType);
      if (config != null && config.getScheme() != null)
         return Producers.of(config.getScheme());
      return defaultScheme;
   }

   @Provides
   @Singleton
   @DefaultHost
   Producer<String> provideDefaultHost()
   {
      return createHost(this.config.getHosts());
   }

   @Provides
   @Singleton
   @WriteHost
   Producer<String> provideWriteHost(@DefaultHost final Producer<String> host)
   {
      return provideHost(OperationType.WRITE, host);
   }

   @Provides
   @Singleton
   @ReadHost
   Producer<String> provideReadHost(@DefaultHost final Producer<String> host)
   {
      return provideHost(OperationType.READ, host);
   }

   @Provides
   @Singleton
   @DeleteHost
   Producer<String> provideDeleteHost(@DefaultHost final Producer<String> host)
   {
      return provideHost(OperationType.DELETE, host);
   }

   private Producer<String> provideHost(
         final OperationType operationType,
         final Producer<String> defaultHost)
   {
      final OperationConfig config = this.config.getOperationConfig().get(operationType);
      if (config != null && config.getHosts() != null)
         return createHost(config.getHosts());
      return defaultHost;
   }

   private Producer<String> createHost(final List<String> hosts)
   {
      final WeightedRandomChoice<String> wrc = new WeightedRandomChoice<String>();
      for (final String host : hosts)
      {
         wrc.addChoice(host);
      }
      return Producers.of(wrc);
   }

   @Provides
   @Singleton
   @DefaultPort
   Producer<Integer> provideDefaultPort()
   {
      if (this.config.getPort() != null)
         return Producers.of(this.config.getPort());
      return null;
   }

   @Provides
   @Singleton
   @WritePort
   Producer<Integer> provideWritePort(@DefaultPort final Producer<Integer> port)
   {
      return providePort(OperationType.WRITE, port);
   }

   @Provides
   @Singleton
   @ReadPort
   Producer<Integer> provideReadPort(@DefaultPort final Producer<Integer> port)
   {
      return providePort(OperationType.READ, port);
   }

   @Provides
   @Singleton
   @DeletePort
   Producer<Integer> provideDeletePort(@DefaultPort final Producer<Integer> port)
   {
      return providePort(OperationType.DELETE, port);
   }

   private Producer<Integer> providePort(
         final OperationType operationType,
         final Producer<Integer> defaultPort)
   {
      final OperationConfig config = this.config.getOperationConfig().get(operationType);
      if (config != null && config.getPort() != null)
         return Producers.of(config.getPort());
      return defaultPort;
   }

   @Provides
   @Singleton
   @DefaultUrlRoot
   Producer<String> provideDefaultUrlRoot()
   {
      if (this.config.getUrlRoot() != null)
      {
         final String root = CharMatcher.is('/').trimFrom(this.config.getUrlRoot());
         if (root.length() > 0)
            return Producers.of(root);
      }
      else
      {
         return Producers.of(this.config.getApi().toString().toLowerCase());
      }
      return null;
   }

   @Provides
   @Singleton
   @DefaultContainer
   Producer<String> provideDefaultContainer()
   {
      return Producers.of(this.config.getContainer());
   }

   @Provides
   @Singleton
   @WriteContainer
   Producer<String> provideWriteContainer(@DefaultContainer final Producer<String> container)
   {
      return provideContainer(OperationType.WRITE, container);
   }

   @Provides
   @Singleton
   @ReadContainer
   Producer<String> provideReadContainer(@DefaultContainer final Producer<String> container)
   {
      return provideContainer(OperationType.READ, container);
   }

   @Provides
   @Singleton
   @DeleteContainer
   Producer<String> provideDeleteContainer(@DefaultContainer final Producer<String> container)
   {
      return provideContainer(OperationType.DELETE, container);
   }

   private Producer<String> provideContainer(
         final OperationType operationType,
         final Producer<String> defaultContainer)
   {
      final OperationConfig config = this.config.getOperationConfig().get(operationType);
      if (config != null && config.getContainer() != null)
         return Producers.of(config.getContainer());
      return defaultContainer;
   }

   @Provides
   @Singleton
   @DefaultQueryParams
   Producer<Map<String, String>> provideDefaultQueryParams()
   {
      final Map<String, String> queryParams = new HashMap<String, String>();
      return Producers.of(queryParams);
   }

   @Provides
   @Singleton
   @WriteQueryParams
   Producer<Map<String, String>> provideWriteQueryParams(
         @DefaultQueryParams final Producer<Map<String, String>> defaultQueryParams)
   {
      return defaultQueryParams;
   }

   @Provides
   @Singleton
   @ReadQueryParams
   Producer<Map<String, String>> provideReadQueryParams(
         @DefaultQueryParams final Producer<Map<String, String>> defaultQueryParams)
   {
      return defaultQueryParams;
   }

   @Provides
   @Singleton
   @DeleteQueryParams
   Producer<Map<String, String>> provideDeleteQueryParams(
         @DefaultQueryParams final Producer<Map<String, String>> defaultQueryParams)
   {
      return defaultQueryParams;
   }

   @Provides
   @Singleton
   @DefaultAuth
   Producer<Pair<String, String>> providesDefaultAuth()
   {
      return createAuth(this.config.getUsername(), this.config.getPassword());
   }

   @Provides
   @Singleton
   @WriteAuth
   Producer<Pair<String, String>> providesWriteAuth(
         @DefaultAuth final Producer<Pair<String, String>> auth)
   {
      return provideAuth(OperationType.WRITE, auth);
   }

   @Provides
   @Singleton
   @ReadAuth
   Producer<Pair<String, String>> providesReadAuth(
         @DefaultAuth final Producer<Pair<String, String>> auth)
   {
      return provideAuth(OperationType.READ, auth);
   }

   @Provides
   @Singleton
   @DeleteAuth
   Producer<Pair<String, String>> providesDeleteAuth(
         @DefaultAuth final Producer<Pair<String, String>> auth)
   {
      return provideAuth(OperationType.DELETE, auth);
   }

   private Producer<Pair<String, String>> provideAuth(
         final OperationType operationType,
         final Producer<Pair<String, String>> defaultAuth)
   {
      final OperationConfig config = this.config.getOperationConfig().get(operationType);
      if (config != null)
      {
         final Producer<Pair<String, String>> auth =
               createAuth(config.getUsername(), config.getPassword());
         if (auth != null)
            return auth;
      }
      return defaultAuth;
   }

   private Producer<Pair<String, String>> createAuth(final String username, final String password)
   {
      if (username != null && password != null)
         return new BasicAuthProducer(username, password);
      else if (username == null && password == null)
         return null;
      throw new IllegalArgumentException("If username is not null password must also be not null");
   }

   @Provides
   @Singleton
   @DefaultHeaders
   List<Producer<Pair<String, String>>> provideDefaultHeaders(
         @DefaultAuth final Producer<Pair<String, String>> auth)
   {
      return addAuth(createHeaders(this.config.getHeaders()), auth);
   }

   @Provides
   @Singleton
   @WriteHeaders
   List<Producer<Pair<String, String>>> provideWriteHeaders(
         @WriteAuth final Producer<Pair<String, String>> auth)
   {
      return provideHeaders(OperationType.WRITE, auth);
   }

   @Provides
   @Singleton
   @ReadHeaders
   List<Producer<Pair<String, String>>> provideReadHeaders(
         @ReadAuth final Producer<Pair<String, String>> auth)
   {
      return provideHeaders(OperationType.READ, auth);
   }

   @Provides
   @Singleton
   @DeleteHeaders
   List<Producer<Pair<String, String>>> provideDeleteHeaders(
         @DeleteAuth final Producer<Pair<String, String>> auth)
   {
      return provideHeaders(OperationType.DELETE, auth);
   }

   private List<Producer<Pair<String, String>>> provideHeaders(
         final OperationType operationType,
         final Producer<Pair<String, String>> auth)
   {
      final Map<String, String> headers = this.config.getHeaders();
      final OperationConfig config = this.config.getOperationConfig().get(operationType);
      if (config != null && config.getHeaders() != null)
         headers.putAll(config.getHeaders());
      return addAuth(createHeaders(headers), auth);
   }

   private List<Producer<Pair<String, String>>> addAuth(
         final List<Producer<Pair<String, String>>> headers,
         final Producer<Pair<String, String>> auth)
   {
      if (auth != null)
         headers.add(auth);
      return headers;
   }

   private List<Producer<Pair<String, String>>> createHeaders(final Map<String, String> headers)
   {
      final List<Producer<Pair<String, String>>> h =
            new ArrayList<Producer<Pair<String, String>>>();
      for (final Entry<String, String> e : headers.entrySet())
      {
         h.add(Producers.of(new Pair<String, String>(e.getKey(), e.getValue())));
      }
      return h;
   }

   @Provides
   @Singleton
   @DefaultEntity
   Producer<Entity> provideDefaultEntity()
   {
      final WeightedRandomChoice<Distribution> wrc = new WeightedRandomChoice<Distribution>();
      for (final FileSize f : this.config.getFilesizes())
      {
         wrc.addChoice(createSizeDistribution(f), f.getWeight());
      }

      final JSONConfiguration config = this.config;
      return new Producer<Entity>()
      {
         private final WeightedRandomChoice<Distribution> sizes = wrc;

         @Override
         public Entity produce(final RequestContext context)
         {
            return Entities.of(config.getSource(), (long) this.sizes.nextChoice().nextSample());
         }
      };
   }

   private static Distribution createSizeDistribution(final FileSize filesize)
   {
      // TODO standardize terminology; mean or average
      final double mean = filesize.getAverage() * filesize.getAverageUnit().toBytes(1);
      final double spread = filesize.getSpread() * filesize.getSpreadUnit().toBytes(1);
      switch (filesize.getDistribution())
      {
      // TODO determine how to expose these in json configuration in a way that makes sense
      // mean/average/min/max?
         case NORMAL :
            return new NormalDistribution(mean, spread);
         case LOGNORMAL :
            return new LogNormalDistribution(mean, spread);
         default :
            return new UniformDistribution(mean, spread);
      }
   }

   @Provides
   @Singleton
   @DefaultMetaData
   Producer<Map<String, String>> provideDefaultMetaData()
   {
      final Map<String, String> metadata = new HashMap<String, String>();
      return Producers.of(metadata);
   }

   @Provides
   @Singleton
   OperationTypeMix provideOperationTypeMix()
   {
      double write = this.config.getWrite();
      final double read = this.config.getRead();
      final double delete = this.config.getDelete();
      if (allEqual(0.0, write, read, delete))
         write = 100.0;

      final long floorBytes = (long) (this.config.getFloor() / 100.0 * this.config.getCapacity());
      final long ceilBytes = (long) (this.config.getCeiling() / 100.0 * this.config.getCapacity());
      return new OperationTypeMix(write, read, delete, floorBytes, ceilBytes);
   }

   private boolean allEqual(final double compare, final double... values)
   {
      final double err = Math.pow(0.1, 6);
      for (final double v : values)
      {
         if (!DoubleMath.fuzzyEquals(v, compare, err))
            return false;
      }
      return true;
   }

   @Provides
   @Singleton
   Scheduler provideScheduler()
   {
      final Concurrency concurrency = this.config.getConcurrency();
      final Distribution count = new UniformDistribution(concurrency.getCount(), 0.0);
      return new RequestRateScheduler(count, concurrency.getUnit());
   }
}
