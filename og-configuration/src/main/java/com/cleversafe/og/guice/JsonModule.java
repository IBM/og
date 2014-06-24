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

package com.cleversafe.og.guice;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import com.cleversafe.og.api.Producer;
import com.cleversafe.og.cli.json.AuthConfig;
import com.cleversafe.og.cli.json.ClientConfig;
import com.cleversafe.og.cli.json.ConcurrencyConfig;
import com.cleversafe.og.cli.json.FileSizeConfig;
import com.cleversafe.og.cli.json.JsonConfig;
import com.cleversafe.og.cli.json.OperationConfig;
import com.cleversafe.og.cli.json.StoppingConditionsConfig;
import com.cleversafe.og.cli.json.enums.AuthType;
import com.cleversafe.og.cli.json.enums.CollectionAlgorithmType;
import com.cleversafe.og.cli.json.enums.ConcurrencyType;
import com.cleversafe.og.distribution.Distribution;
import com.cleversafe.og.distribution.NormalDistribution;
import com.cleversafe.og.distribution.UniformDistribution;
import com.cleversafe.og.guice.annotation.DefaultContainer;
import com.cleversafe.og.guice.annotation.DefaultEntity;
import com.cleversafe.og.guice.annotation.DefaultHeaders;
import com.cleversafe.og.guice.annotation.DefaultHost;
import com.cleversafe.og.guice.annotation.DefaultId;
import com.cleversafe.og.guice.annotation.DefaultObjectLocation;
import com.cleversafe.og.guice.annotation.DefaultPort;
import com.cleversafe.og.guice.annotation.DefaultQueryParams;
import com.cleversafe.og.guice.annotation.DefaultScheme;
import com.cleversafe.og.guice.annotation.DefaultUriRoot;
import com.cleversafe.og.guice.annotation.DeleteHeaders;
import com.cleversafe.og.guice.annotation.DeleteHost;
import com.cleversafe.og.guice.annotation.DeleteWeight;
import com.cleversafe.og.guice.annotation.ReadHeaders;
import com.cleversafe.og.guice.annotation.ReadHost;
import com.cleversafe.og.guice.annotation.ReadWeight;
import com.cleversafe.og.guice.annotation.WriteHeaders;
import com.cleversafe.og.guice.annotation.WriteHost;
import com.cleversafe.og.guice.annotation.WriteWeight;
import com.cleversafe.og.http.Scheme;
import com.cleversafe.og.http.auth.BasicAuth;
import com.cleversafe.og.http.auth.HttpAuth;
import com.cleversafe.og.http.util.ApiType;
import com.cleversafe.og.operation.Entity;
import com.cleversafe.og.s3.auth.AWSAuthV2;
import com.cleversafe.og.scheduling.ConcurrentRequestScheduler;
import com.cleversafe.og.scheduling.RequestRateScheduler;
import com.cleversafe.og.scheduling.Scheduler;
import com.cleversafe.og.util.Entities;
import com.cleversafe.og.util.OperationType;
import com.cleversafe.og.util.Pair;
import com.cleversafe.og.util.WeightedRandomChoice;
import com.cleversafe.og.util.producer.Producers;
import com.google.common.base.CharMatcher;
import com.google.common.math.DoubleMath;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class JsonModule extends AbstractModule
{
   private final JsonConfig config;
   private final static double err = Math.pow(0.1, 6);

   public JsonModule(final JsonConfig config)
   {
      this.config = checkNotNull(config);
   }

   @Override
   protected void configure()
   {}

   @Provides
   public JsonConfig provideJsonConfig()
   {
      return this.config;
   }

   @Provides
   @Singleton
   @DefaultId
   public Producer<Long> provideDefaultIdProducer()
   {
      return new Producer<Long>()
      {
         private final AtomicLong id = new AtomicLong();

         @Override
         public Long produce()
         {
            return this.id.getAndIncrement();
         }
      };
   }

   @Provides
   @Singleton
   @DefaultScheme
   public Producer<Scheme> provideDefaultScheme()
   {
      return Producers.of(this.config.getScheme());
   }

   @Provides
   @Singleton
   @DefaultHost
   public Producer<String> provideDefaultHost()
   {
      return createHost(this.config.getHostAlgorithm(), this.config.getHosts());
   }

   @Provides
   @Singleton
   @WriteHost
   public Producer<String> provideWriteHost(@DefaultHost final Producer<String> host)
   {
      return provideHost(OperationType.WRITE, host);
   }

   @Provides
   @Singleton
   @ReadHost
   public Producer<String> provideReadHost(@DefaultHost final Producer<String> host)
   {
      return provideHost(OperationType.READ, host);
   }

   @Provides
   @Singleton
   @DeleteHost
   public Producer<String> provideDeleteHost(@DefaultHost final Producer<String> host)
   {
      return provideHost(OperationType.DELETE, host);
   }

   private Producer<String> provideHost(
         final OperationType operationType,
         final Producer<String> defaultHost)
   {
      final OperationConfig config = this.config.getOperationConfig().get(operationType);
      if (config != null && config.getHosts() != null)
         return createHost(config.getHostAlgorithm(), config.getHosts());
      return defaultHost;
   }

   private Producer<String> createHost(
         final CollectionAlgorithmType algorithm,
         final List<String> hosts)
   {
      if (CollectionAlgorithmType.ROUNDROBIN == algorithm)
         return Producers.roundRobin(hosts);
      else
      {
         final WeightedRandomChoice<String> wrc = new WeightedRandomChoice<String>();
         for (final String host : hosts)
         {
            wrc.addChoice(host);
         }
         return Producers.of(wrc);
      }
   }

   @Provides
   @Singleton
   @DefaultPort
   public Producer<Integer> provideDefaultPort()
   {
      if (this.config.getPort() != null)
         return Producers.of(this.config.getPort());
      return null;
   }

   @Provides
   public ApiType provideApi()
   {
      return this.config.getApi();
   }

   @Provides
   @Singleton
   @DefaultUriRoot
   public Producer<String> provideDefaultUriRoot()
   {
      if (this.config.getUriRoot() != null)
      {
         final String root = CharMatcher.is('/').trimFrom(this.config.getUriRoot());
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
   public Producer<String> provideDefaultContainer()
   {
      return Producers.of(this.config.getContainer());
   }

   @Provides
   @Singleton
   @DefaultQueryParams
   public Producer<Map<String, String>> provideDefaultQueryParams()
   {
      final Map<String, String> queryParams = new HashMap<String, String>();
      return Producers.of(queryParams);
   }

   @Provides
   @Singleton
   public HttpAuth providesDefaultAuth()
   {
      final AuthConfig authConfig = this.config.getAuthentication();
      final AuthType authType = authConfig.getAuthType();
      final String username = authConfig.getUsername();
      final String password = authConfig.getPassword();

      if (username != null && password != null)
      {
         if (AuthType.AWSV2 == authType)
            return new AWSAuthV2(Producers.of(username), Producers.of(password));
         else
            return new BasicAuth(Producers.of(username), Producers.of(password));
      }
      else if (username == null && password == null)
         return null;
      throw new IllegalArgumentException("If username is not null password must also be not null");
   }

   @Provides
   public StoppingConditionsConfig provideStoppingConditionsConfig()
   {
      return this.config.getStoppingConditions();
   }

   @Provides
   @Singleton
   @DefaultHeaders
   public List<Producer<Pair<String, String>>> provideDefaultHeaders()
   {
      return createHeaders(this.config.getHeaders());
   }

   @Provides
   @Singleton
   @WriteHeaders
   public List<Producer<Pair<String, String>>> provideWriteHeaders()
   {
      return provideHeaders(OperationType.WRITE);
   }

   @Provides
   @Singleton
   @ReadHeaders
   public List<Producer<Pair<String, String>>> provideReadHeaders()
   {
      return provideHeaders(OperationType.READ);
   }

   @Provides
   @Singleton
   @DeleteHeaders
   public List<Producer<Pair<String, String>>> provideDeleteHeaders()
   {
      return provideHeaders(OperationType.DELETE);
   }

   private List<Producer<Pair<String, String>>> provideHeaders(final OperationType operationType)
   {
      final Map<String, String> headers = this.config.getHeaders();
      final OperationConfig config = this.config.getOperationConfig().get(operationType);
      if (config != null && config.getHeaders() != null)
         headers.putAll(config.getHeaders());
      return createHeaders(headers);
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
   public Producer<Entity> provideDefaultEntity()
   {
      final WeightedRandomChoice<Distribution> wrc = new WeightedRandomChoice<Distribution>();
      for (final FileSizeConfig f : this.config.getFilesizes())
      {
         wrc.addChoice(createSizeDistribution(f), f.getWeight());
      }

      final JsonConfig config = this.config;
      return new Producer<Entity>()
      {
         private final WeightedRandomChoice<Distribution> sizes = wrc;

         @Override
         public Entity produce()
         {
            return Entities.of(config.getSource(), (long) this.sizes.nextChoice().nextSample());
         }
      };
   }

   private static Distribution createSizeDistribution(final FileSizeConfig filesize)
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
         default :
            return new UniformDistribution(mean, spread);
      }
   }

   // TODO simplify this method if possible
   @Provides
   @Singleton
   @DefaultObjectLocation
   public String provideObjectLocation() throws IOException
   {

      String path = this.config.getObjectLocation();
      if (path == null || path.length() == 0)
         path = "./object";

      final File f = new File(path).getCanonicalFile();
      if (!f.exists())
      {
         final boolean created = f.mkdirs();
         if (!created)
            throw new RuntimeException(String.format(
                  "Failed to create object location directory [%s]", f.toString()));
      }
      else if (!f.isDirectory())
      {
         throw new RuntimeException(String.format("Object location is not a directory [%s]",
               f.toString()));
      }
      return f.toString();
   }

   @Provides
   @WriteWeight
   public double provideWriteWeight()
   {
      final double write = this.config.getWrite();
      checkArgument(inRange(write), "write must be in range [0.0, 100.0] [%s]", write);
      final double read = this.config.getRead();
      final double delete = this.config.getDelete();
      if (allEqual(0.0, write, read, delete))
         return 100.0;
      return write;
   }

   @Provides
   @ReadWeight
   public double provideReadWeight()
   {
      final double read = this.config.getRead();
      checkArgument(inRange(read), "read must be in range [0.0, 100.0] [%s]", read);
      return read;
   }

   @Provides
   @DeleteWeight
   public double provideDeleteWeight()
   {
      final double delete = this.config.getDelete();
      checkArgument(inRange(delete), "delete must be in range [0.0, 100.0] [%s]", delete);
      return delete;
   }

   private boolean inRange(final double v)
   {
      return DoubleMath.fuzzyCompare(v, 0.0, err) >= 0
            && DoubleMath.fuzzyCompare(v, 100.0, err) <= 0;
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
   public Scheduler provideScheduler()
   {
      final ConcurrencyConfig concurrency = this.config.getConcurrency();
      if (ConcurrencyType.THREADS == concurrency.getType())
         return new ConcurrentRequestScheduler(Math.round(concurrency.getCount()));
      else
      {
         final Distribution count = new UniformDistribution(concurrency.getCount(), 0.0);
         return new RequestRateScheduler(count, concurrency.getUnit());
      }
   }

   @Provides
   public ClientConfig provideClientConfig()
   {
      return this.config.getClient();
   }
}
