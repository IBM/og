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

import com.cleversafe.og.guice.annotation.DeleteHeaders;
import com.cleversafe.og.guice.annotation.DeleteHost;
import com.cleversafe.og.guice.annotation.DeleteWeight;
import com.cleversafe.og.guice.annotation.ReadHeaders;
import com.cleversafe.og.guice.annotation.ReadHost;
import com.cleversafe.og.guice.annotation.ReadWeight;
import com.cleversafe.og.guice.annotation.TestContainer;
import com.cleversafe.og.guice.annotation.TestEntity;
import com.cleversafe.og.guice.annotation.TestHeaders;
import com.cleversafe.og.guice.annotation.TestHost;
import com.cleversafe.og.guice.annotation.TestId;
import com.cleversafe.og.guice.annotation.TestObjectFileLocation;
import com.cleversafe.og.guice.annotation.TestObjectFileName;
import com.cleversafe.og.guice.annotation.TestPassword;
import com.cleversafe.og.guice.annotation.TestPort;
import com.cleversafe.og.guice.annotation.TestQueryParams;
import com.cleversafe.og.guice.annotation.TestScheme;
import com.cleversafe.og.guice.annotation.TestUriRoot;
import com.cleversafe.og.guice.annotation.TestUsername;
import com.cleversafe.og.guice.annotation.WriteHeaders;
import com.cleversafe.og.guice.annotation.WriteHost;
import com.cleversafe.og.guice.annotation.WriteWeight;
import com.cleversafe.og.http.Scheme;
import com.cleversafe.og.http.auth.BasicAuth;
import com.cleversafe.og.http.auth.HttpAuth;
import com.cleversafe.og.http.util.Api;
import com.cleversafe.og.json.ClientConfig;
import com.cleversafe.og.json.ConcurrencyConfig;
import com.cleversafe.og.json.FilesizeConfig;
import com.cleversafe.og.json.HostConfig;
import com.cleversafe.og.json.JsonConfig;
import com.cleversafe.og.json.OperationConfig;
import com.cleversafe.og.json.StoppingConditionsConfig;
import com.cleversafe.og.json.enums.AuthType;
import com.cleversafe.og.json.enums.CollectionAlgorithmType;
import com.cleversafe.og.json.enums.ConcurrencyType;
import com.cleversafe.og.json.enums.DistributionType;
import com.cleversafe.og.operation.Entity;
import com.cleversafe.og.operation.EntityType;
import com.cleversafe.og.s3.auth.AWSAuthV2;
import com.cleversafe.og.scheduling.ConcurrentRequestScheduler;
import com.cleversafe.og.scheduling.RequestRateScheduler;
import com.cleversafe.og.scheduling.Scheduler;
import com.cleversafe.og.util.Entities;
import com.cleversafe.og.util.distribution.Distribution;
import com.cleversafe.og.util.distribution.LogNormalDistribution;
import com.cleversafe.og.util.distribution.NormalDistribution;
import com.cleversafe.og.util.distribution.UniformDistribution;
import com.cleversafe.og.util.producer.Producer;
import com.cleversafe.og.util.producer.Producers;
import com.cleversafe.og.util.producer.RandomChoiceProducer;
import com.google.common.base.CharMatcher;
import com.google.common.collect.Range;
import com.google.common.eventbus.EventBus;
import com.google.common.math.DoubleMath;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class JsonModule extends AbstractModule
{
   private final JsonConfig config;
   private static final double ERR = Math.pow(0.1, 6);
   private static final Range<Double> PERCENTAGE = Range.closed(0.0, 100.0);

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
   @TestId
   public Producer<String> provideTestIdProducer()
   {
      return new Producer<String>()
      {
         private final AtomicLong id = new AtomicLong();

         @Override
         public String produce()
         {
            return String.valueOf(this.id.getAndIncrement());
         }
      };
   }

   @Provides
   @Singleton
   @TestScheme
   public Producer<Scheme> provideTestScheme()
   {
      return Producers.of(this.config.getScheme());
   }

   @Provides
   @Singleton
   @TestHost
   public Producer<String> provideTesttHost()
   {
      return createHost(this.config.getHostSelection(), this.config.getHost());
   }

   @Provides
   @Singleton
   @WriteHost
   public Producer<String> provideWriteHost(@TestHost final Producer<String> host)
   {
      return provideHost(this.config.getWrite(), host);
   }

   @Provides
   @Singleton
   @ReadHost
   public Producer<String> provideReadHost(@TestHost final Producer<String> host)
   {
      return provideHost(this.config.getRead(), host);
   }

   @Provides
   @Singleton
   @DeleteHost
   public Producer<String> provideDeleteHost(@TestHost final Producer<String> host)
   {
      return provideHost(this.config.getDelete(), host);
   }

   private Producer<String> provideHost(
         final OperationConfig operationConfig,
         final Producer<String> testHost)
   {
      if (operationConfig != null)
      {
         final List<HostConfig> operationHosts = operationConfig.getHost();
         if (operationHosts != null && !operationHosts.isEmpty())
            return createHost(operationConfig.getHostAlgorithm(), operationHosts);
      }
      return testHost;
   }

   private Producer<String> createHost(
         final CollectionAlgorithmType algorithm,
         final List<HostConfig> hosts)
   {
      if (CollectionAlgorithmType.ROUNDROBIN == algorithm)
      {
         final List<String> hostList = new ArrayList<String>();
         for (final HostConfig host : hosts)
         {
            hostList.add(host.getHost());
         }
         return Producers.cycle(hostList);
      }

      else
      {
         final RandomChoiceProducer.Builder<String> wrc =
               new RandomChoiceProducer.Builder<String>();
         for (final HostConfig host : hosts)
         {
            wrc.withChoice(host.getHost(), host.getWeight());
         }
         return wrc.build();
      }
   }

   @Provides
   @Singleton
   @TestPort
   public Producer<Integer> provideTestPort()
   {
      if (this.config.getPort() != null)
         return Producers.of(this.config.getPort());
      return null;
   }

   @Provides
   public Api provideApi()
   {
      return this.config.getApi();
   }

   @Provides
   @Singleton
   @TestUriRoot
   public Producer<String> provideTestUriRoot()
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
   @TestContainer
   public Producer<String> provideTestContainer()
   {
      return Producers.of(this.config.getContainer());
   }

   @Provides
   @Singleton
   @TestQueryParams
   public Map<String, String> provideTestQueryParams()
   {
      return new HashMap<String, String>();
   }

   @Provides
   @Singleton
   @TestUsername
   public Producer<String> provideTestUsername()
   {
      if (this.config.getAuthentication().getUsername() != null)
         return Producers.of(this.config.getAuthentication().getUsername());
      return null;
   }

   @Provides
   @Singleton
   @TestPassword
   public Producer<String> provideTestPassword()
   {
      if (this.config.getAuthentication().getPassword() != null)
         return Producers.of(this.config.getAuthentication().getPassword());
      return null;
   }

   @Provides
   @Singleton
   public HttpAuth providesTestAuth(
         @TestUsername final Producer<String> username,
         @TestPassword final Producer<String> password)
   {
      final AuthType type = this.config.getAuthentication().getType();

      if (username != null && password != null)
      {
         if (AuthType.AWSV2 == type)
            return new AWSAuthV2();
         else
            return new BasicAuth();
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
   @TestHeaders
   public Map<Producer<String>, Producer<String>> provideTestHeaders()
   {
      return createHeaders(this.config.getHeaders());
   }

   @Provides
   @Singleton
   @WriteHeaders
   public Map<Producer<String>, Producer<String>> provideWriteHeaders()
   {
      return provideHeaders(this.config.getWrite());
   }

   @Provides
   @Singleton
   @ReadHeaders
   public Map<Producer<String>, Producer<String>> provideReadHeaders()
   {
      return provideHeaders(this.config.getRead());
   }

   @Provides
   @Singleton
   @DeleteHeaders
   public Map<Producer<String>, Producer<String>> provideDeleteHeaders()
   {
      return provideHeaders(this.config.getDelete());
   }

   private Map<Producer<String>, Producer<String>> provideHeaders(
         final OperationConfig operationConfig)
   {
      final Map<String, String> headers = this.config.getHeaders();
      if (operationConfig != null && operationConfig.getHeaders() != null)
         headers.putAll(this.config.getHeaders());
      return createHeaders(headers);
   }

   private Map<Producer<String>, Producer<String>> createHeaders(final Map<String, String> headers)
   {
      final Map<Producer<String>, Producer<String>> h =
            new HashMap<Producer<String>, Producer<String>>();
      for (final Entry<String, String> e : headers.entrySet())
      {
         h.put(Producers.of(e.getKey()), Producers.of(e.getValue()));
      }
      return h;
   }

   @Provides
   @Singleton
   @TestEntity
   public Producer<Entity> provideTestEntity()
   {
      if (CollectionAlgorithmType.ROUNDROBIN == this.config.getFilesizeSelection())
      {
         final List<Distribution> distributions = new ArrayList<Distribution>();
         for (final FilesizeConfig f : this.config.getFilesize())
         {
            distributions.add(createSizeDistribution(f));
         }
         return createEntityProducer(Producers.cycle(distributions));
      }

      final RandomChoiceProducer.Builder<Distribution> wrc =
            new RandomChoiceProducer.Builder<Distribution>();
      for (final FilesizeConfig f : this.config.getFilesize())
      {
         wrc.withChoice(createSizeDistribution(f), f.getWeight());
      }
      return createEntityProducer(wrc.build());
   }

   private static Distribution createSizeDistribution(final FilesizeConfig filesize)
   {
      final double average = filesize.getAverage() * filesize.getAverageUnit().toBytes(1);
      final double spread = filesize.getSpread() * filesize.getSpreadUnit().toBytes(1);
      if (DistributionType.NORMAL == filesize.getDistribution())
         return new NormalDistribution(average, spread);
      else if (DistributionType.LOGNORMAL == filesize.getDistribution())
         return new LogNormalDistribution(average, spread);
      return new UniformDistribution(average, spread);
   }

   private Producer<Entity> createEntityProducer(final Producer<Distribution> distributionProducer)
   {
      final JsonConfig jsonConfig = this.config;

      return new Producer<Entity>()
      {
         @Override
         public Entity produce()
         {
            final long sample = (long) distributionProducer.produce().nextSample();

            if (EntityType.ZEROES == jsonConfig.getSource())
               return Entities.zeroes(sample);
            else
               return Entities.random(sample);
         }
      };
   }

   // TODO simplify this method if possible
   @Provides
   @Singleton
   @TestObjectFileLocation
   public String provideObjectFileLocation() throws IOException
   {

      String path = this.config.getObjectManager().getObjectFileLocation();
      if (path == null || path.length() == 0)
         path = "./object";

      final File f = new File(path).getCanonicalFile();
      if (!f.exists())
      {
         final boolean created = f.mkdirs();
         if (!created)
            throw new RuntimeException(String.format(
                  "Failed to create object location directory [%s]", f));
      }
      else if (!f.isDirectory())
      {
         throw new RuntimeException(String.format("Object location is not a directory [%s]",
               f.toString()));
      }
      return f.toString();
   }

   @Provides
   @Singleton
   @TestObjectFileName
   public String provideObjectFileName(
         @TestContainer final Producer<String> container,
         final Api api)
   {
      if (this.config.getObjectManager().getObjectFileName() != null)
         return this.config.getObjectManager().getObjectFileName();
      // FIXME this naming scheme will break unless @TestContainer is a constant producer
      return container.produce() + "-" + api.toString().toLowerCase();
   }

   @Provides
   @WriteWeight
   public double provideWriteWeight()
   {
      final double write = this.config.getWrite().getWeight();
      checkArgument(PERCENTAGE.contains(write), "write must be in range [0.0, 100.0] [%s]", write);
      final double read = this.config.getRead().getWeight();
      final double delete = this.config.getDelete().getWeight();
      if (allEqual(0.0, write, read, delete))
         return 100.0;
      return write;
   }

   @Provides
   @ReadWeight
   public double provideReadWeight()
   {
      final double read = this.config.getRead().getWeight();
      checkArgument(PERCENTAGE.contains(read), "read must be in range [0.0, 100.0] [%s]", read);
      return read;
   }

   @Provides
   @DeleteWeight
   public double provideDeleteWeight()
   {
      final double delete = this.config.getDelete().getWeight();
      checkArgument(PERCENTAGE.contains(delete), "delete must be in range [0.0, 100.0] [%s]",
            delete);
      return delete;
   }

   private boolean allEqual(final double compare, final double... values)
   {
      for (final double v : values)
      {
         if (!DoubleMath.fuzzyEquals(v, compare, ERR))
            return false;
      }
      return true;
   }

   @Provides
   @Singleton
   public Scheduler provideScheduler(final EventBus eventBus)
   {
      final ConcurrencyConfig concurrency = this.config.getConcurrency();
      if (ConcurrencyType.THREADS == concurrency.getType())
      {
         final Scheduler scheduler =
               new ConcurrentRequestScheduler((int) Math.round(concurrency.getCount()));
         eventBus.register(scheduler);
         return scheduler;
      }
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
